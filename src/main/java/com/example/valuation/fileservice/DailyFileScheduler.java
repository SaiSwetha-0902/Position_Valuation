package com.example.valuation.fileservice;

import com.example.valuation.dao.ValuationDao;
import com.example.valuation.entity.ValuationEntity;
import com.example.valuation.entity.ValuationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DailyFileScheduler {

    @Autowired
    private ValuationDao valuationDao;

    @Autowired
    private S3Client s3Client;

    private static final String BUCKET_NAME = "recon-buckets";

    // @Scheduled(cron = "0 0 1 * * ?") // Uncomment for nightly 1 AM job
    @Scheduled(fixedDelay = 10000) // For testing, every 10 sec
    public void runDailyJob() {

        LocalDate today = LocalDate.now();
        log.info("Starting daily valuation file job for {}", today);

        // Fetch only NEW records
        List<ValuationEntity> newRecords =
                valuationDao.findByStatusAndValuationDate(ValuationStatus.NEW, today);

        if (newRecords.isEmpty()) {
            log.info("No NEW valuation records found for {}", today);
            return;
        }

        // Group by fundNumber
        Map<Integer, List<ValuationEntity>> recordsByFund =
                newRecords.stream()
                        .collect(Collectors.groupingBy(ValuationEntity::getFundNumber));

        for (Map.Entry<Integer, List<ValuationEntity>> entry : recordsByFund.entrySet()) {

            Integer fundNumber = entry.getKey();
            List<ValuationEntity> fundRecords = entry.getValue();

            String fileName = "valuation_" + today + "_fund_" + String.format("%04d", fundNumber) + ".txt";
            File localFile = new File(fileName);
            UUID fileId = UUID.randomUUID();

            try {
                // Write file locally
                writeFile(localFile, fundRecords);

                // Upload file to S3
                s3Client.putObject(
                        PutObjectRequest.builder()
                                .bucket(BUCKET_NAME)
                                .key(fileName)
                                .build(),
                        Paths.get(localFile.getAbsolutePath())
                );

                // Mark records FILED in memory
                for (ValuationEntity v : fundRecords) {
                    v.setStatus(ValuationStatus.FILED);
                    v.setFileId(fileId);
                }

                // Save status to DB safely
                try {
                    valuationDao.saveAll(fundRecords);
                } catch (Exception dbEx) {
                    log.error("Failed to update FILED status in DB for fund {}", fundNumber, dbEx);
                    for (ValuationEntity v : fundRecords) {
                        sendStatusDLQ(v, dbEx);
                    }
                }

                log.info("Successfully filed {} records for fund {}", fundRecords.size(), fundNumber);

            } catch (Exception fileEx) {
                // File write / S3 upload failed
                log.error("File write/upload failed for fund {}", fundNumber, fileEx);

                // Mark all records FAILED in memory
                for (ValuationEntity v : fundRecords) {
                    v.setStatus(ValuationStatus.FAILED);
                }

                // Try to persist FAILED status
                try {
                    valuationDao.saveAll(fundRecords);
                } catch (Exception dbEx) {
                    log.error("Failed to update FAILED status in DB for fund {}", fundNumber, dbEx);
                    for (ValuationEntity v : fundRecords) {
                        sendStatusDLQ(v, dbEx);
                    }
                }

                // Send file-level DLQ
                sendFileDLQ(fundNumber, today, fundRecords, fileEx);

            } finally {
                // Delete temp file
                if (localFile.exists()) {
                    localFile.delete();
                }
            }
        }
    }

    /* ================= FILE WRITING ================= */
    private void writeFile(File file, List<ValuationEntity> records) throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (ValuationEntity rec : records) {
                writer.write(buildLine(rec));
                writer.newLine();
            }
        }
    }

    private String buildLine(ValuationEntity rec) {
        boolean rejected = rec.getRejectReason() != null && !rec.getRejectReason().isBlank();
        String recordType = rejected ? "120" : "005";
        String orderRef = padRight(rec.getRawOrderId().toString(), 36);
        String distId = padLeftNumeric(rec.getFirmNumber(), 4);
        String fundId = padLeftNumeric(rec.getFundNumber(), 4);
        String qty = padLeftDecimal(rec.getShareQuantity(), 10, 6);
        String nav = padLeftDecimal(rec.getNavValue(), 10, 4);
        String amountDue = padLeftDecimal(rec.getValuationAmount(), 10, 4);
        String currency = padRight("USD", 3);
        String date = rec.getTradeDateTime().toLocalDate().toString().replace("-", "");
        String transRef = padRight(rec.getTransactionId(), 16);
        String rejectReason = rejected ? padRight(rec.getRejectReason(), 30) : "";
        String txnType = rec.getTransactionType();
        String type = padRight(
                "SELL".equalsIgnoreCase(txnType) ? "SEL" :
                        "BUY".equalsIgnoreCase(txnType) ? "BUY" : txnType, 3);

        String line = recordType + orderRef + distId + fundId + qty + nav +
                amountDue + currency + date + transRef + rejectReason + type;

        int expectedLength = rejected ? 137 : 107;
        if (line.length() != expectedLength) {
            throw new RuntimeException("Invalid record length " + line.length() +
                    ", expected " + expectedLength +
                    ", transactionId=" + rec.getTransactionId());
        }
        return line;
    }

    private String padRight(String s, int len) {
        if (s == null) s = "";
        return String.format("%1$-" + len + "s", s).substring(0, len);
    }

    private String padLeftNumeric(Integer val, int len) {
        if (val == null) val = 0;
        return String.format("%0" + len + "d", val);
    }

    private String padLeftDecimal(BigDecimal num, int len, int decimals) {
        if (num == null) num = BigDecimal.ZERO;
        num = num.setScale(decimals);
        return String.format("%0" + len + "." + decimals + "f", num);
    }

    private void sendFileDLQ(Integer firmNumber, LocalDate date, List<ValuationEntity> records, Exception e) {
        log.error("FILE_DLQ → firm={}, date={}, count={}, reason={}",
                firmNumber, date, records.size(), e.getMessage());
        // Hook to central publisher or monitoring system
    }

    private void sendStatusDLQ(ValuationEntity v, Exception e) {
        log.error("STATUS_DLQ → valuationId={}, attemptedStatus={}, reason={}",
                v.getId(), v.getStatus(), e.getMessage());
        // Hook to central publisher or monitoring system
    }
}
