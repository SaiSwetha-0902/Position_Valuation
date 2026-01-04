package com.example.valuation.dao;

import com.example.valuation.entity.PositionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PositionHistoryRepository
        extends JpaRepository<PositionHistory, Long> {

    List<PositionHistory> findByDistributorIdAndFundIdOrderBySnapshotDateDesc(
            Long distributorId,
            Long fundId);

    List<PositionHistory> findByDistributorIdOrderBySnapshotDateDesc(
            Long distributorId);

    List<PositionHistory> findByFundIdOrderBySnapshotDateDesc(
            Long fundId);

    List<PositionHistory> findBySnapshotDateBetween(
            LocalDate startDate,
            LocalDate endDate);
}
