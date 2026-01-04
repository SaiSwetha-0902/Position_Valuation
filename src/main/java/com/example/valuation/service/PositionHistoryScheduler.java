package com.example.valuation.service;

import com.example.valuation.model.*;
import com.example.valuation.service.*;
import com.example.valuation.entity.*;

import com.example.valuation.dao.*;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.example.valuation.dao.PositionHistoryRepository;
import com.example.valuation.dao.PositionRepository;
import com.example.valuation.dto.NavRecordDTO;
import com.example.valuation.entity.Position;
import com.example.valuation.entity.PositionHistory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PositionHistoryScheduler {

    private final PositionRepository positionRepository;
    private final PositionHistoryRepository positionHistoryRepository;
    private final NavCacheService navService;

    public PositionHistoryScheduler(
            PositionRepository positionRepository,
            PositionHistoryRepository positionHistoryRepository,
            NavCacheService navService) {
        this.positionRepository = positionRepository;
        this.positionHistoryRepository = positionHistoryRepository;
        this.navService = navService;
    }

    /**
     * EOD snapshot at 16:00
     */
    @Scheduled(cron = "0 5 17 * * *")
    public void capturePositionSnapshot() {

        LocalDate snapshotDate = LocalDate.now();
        LocalDateTime createdAt = LocalDateTime.now();

        List<Position> allPositions = positionRepository.findAll();

        for (Position position : allPositions) {

            Long distributorId = position.getId().getDistributorId();
            Long fundId = position.getId().getFundId();

            try {
                // Fetch latest NAV
                NavRecordDTO nav = navService.getNavByFundId(fundId);

                BigDecimal navValue = BigDecimal.valueOf(nav.getNav());

                BigDecimal quantity = position.getQuantity();
                BigDecimal totalValue = quantity.multiply(navValue);

                PositionHistory history = PositionHistory.builder()
                        .distributorId(distributorId)
                        .fundId(fundId)
                        .quantity(quantity)
                        .navValue(navValue)
                        .totalValue(totalValue)
                        .snapshotDate(snapshotDate)
                        .createdAt(createdAt)
                        .build();

                positionHistoryRepository.save(history);

            } catch (Exception e) {
                // Never break scheduler
                System.err.println(
                        "Skipping snapshot for fundId " + fundId + " : " + e.getMessage());
            }
        }
    }
}
