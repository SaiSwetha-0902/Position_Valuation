package com.example.valuation.listener;

import com.example.valuation.dao.ValuationDao;
import com.example.valuation.entity.ValuationEntity;
import com.example.valuation.event.ConfirmedTradeEvent;
import com.example.valuation.service.PositionService;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
public class PositionEventListener {

    private final ValuationDao valuationDao;
    private final PositionService positionService;

    public PositionEventListener(ValuationDao valuationDao,
            PositionService positionService) {
        this.valuationDao = valuationDao;
        this.positionService = positionService;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleConfirmedTrade(ConfirmedTradeEvent event) {

        ValuationEntity valuation = valuationDao.findById(event.getValuationId())
                .orElseThrow();

        positionService.applyConfirmedTrade(valuation);
    }
}