package com.example.valuation.service;

import com.example.valuation.dto.NavRecordDTO;
import com.example.valuation.dto.PositionResponse;
import com.example.valuation.entity.Position;
import com.example.valuation.entity.PositionId;
import com.example.valuation.entity.ValuationEntity;
import com.example.valuation.dao.PositionRepository;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PositionService {

    private final PositionRepository positionRepository;
    private final NavCacheService navCacheService;

    public PositionService(PositionRepository positionRepository,
                           NavCacheService navCacheService) {
        this.positionRepository = positionRepository;
        this.navCacheService = navCacheService;
    }

    /*
     * =======================================================
     * WRITE PATH (TRADE-DRIVEN)
     * =======================================================
     */

    @Transactional
    public void applyConfirmedTrade(ValuationEntity valuation) {

        Long distributorId = valuation.getFirmNumber().longValue();
        Long fundId = valuation.getFundNumber().longValue();

        Position position = positionRepository.findForUpdate(distributorId, fundId)
                .orElseGet(() -> {
                    Position p = new Position();
                    p.setId(new PositionId(distributorId, fundId));
                    p.setQuantity(BigDecimal.ZERO);
                    p.setNavValue(BigDecimal.ZERO);
                    p.setTotalValue(BigDecimal.ZERO);
                    p.setLastUpdated(LocalDateTime.now());
                    return p;
                });

        BigDecimal qty = valuation.getShareQuantity();

        if ("BUY".equalsIgnoreCase(valuation.getTransactionType())) {
            position.setQuantity(position.getQuantity().add(qty));

        } else if ("SELL".equalsIgnoreCase(valuation.getTransactionType())) {
            if (position.getQuantity().compareTo(qty) < 0) {
                throw new RuntimeException("Insufficient units to sell");
            }
            position.setQuantity(position.getQuantity().subtract(qty));
        }

        // NAV COMES FROM VALUATION (trade day)
        BigDecimal nav = valuation.getNavValue();

        position.setNavValue(nav);
        position.setTotalValue(position.getQuantity().multiply(nav));
        position.setLastUpdated(LocalDateTime.now());

        positionRepository.save(position);
    }

    /*
     * =======================================================
     * NAV-DRIVEN UPDATE (NO TRADES REQUIRED)
     * Uses Redis structure: HSET <fundId> date time nav
     * =======================================================
     */

    @Transactional
    public void updatePositionsUsingRedisNav(Long fundId) {

        NavRecordDTO navDto = navCacheService.getNavByFundId(fundId);
        BigDecimal nav = BigDecimal.valueOf(navDto.getNav());

        List<Position> positions = positionRepository.findByIdFundId(fundId);

        for (Position position : positions) {
            position.setNavValue(nav);
            position.setTotalValue(position.getQuantity().multiply(nav));
            position.setLastUpdated(LocalDateTime.now());
        }

        positionRepository.saveAll(positions);
    }

    /*
     * =======================================================
     * SCHEDULER – DAILY @ 6:30 PM
     * =======================================================
     */

    @Scheduled(cron = "0 30 18 * * ?")
    public void dailyNavUpdateJob() {

        List<Long> fundIds = positionRepository.findDistinctFundIds();

        for (Long fundId : fundIds) {
            updatePositionsUsingRedisNav(fundId);
        }
    }

    /*
     * =======================================================
     * READ PATHS
     * =======================================================
     */

    public List<PositionResponse> getAllPositions() {
        return positionRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public PositionResponse getPosition(Long distributorId, Long fundId) {

        PositionId id = new PositionId(distributorId, fundId);

        Position position = positionRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Position not found for distributor=" +
                                        distributorId + ", fund=" + fundId));

        return mapToResponse(position);
    }

    public List<PositionResponse> getDistributorPositions(Long distributorId) {
        return positionRepository.findByIdDistributorId(distributorId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<PositionResponse> getFundPositions(Long fundId) {
        return positionRepository.findByIdFundId(fundId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /*
     * =======================================================
     * MAPPER
     * =======================================================
     */

    private PositionResponse mapToResponse(Position position) {
        return PositionResponse.builder()
                .distributorId(position.getId().getDistributorId())
                .fundId(position.getId().getFundId())
                .quantity(position.getQuantity())
                .navValue(position.getNavValue())
                .totalValue(position.getTotalValue())
                .lastUpdated(position.getLastUpdated())
                .build();
    }
}