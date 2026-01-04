package com.example.valuation.dao;

import com.example.valuation.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface PositionRepository
        extends JpaRepository<Position, PositionId> {

    // 🔒 WRITE PATH (used ONLY by ValuationService)
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("""
        select p from Position p
        where p.id.distributorId = :distributorId
        and p.id.fundId = :fundId
        """)
        Optional<Position> findForUpdate(Long distributorId, Long fundId);



    // 📖 READ PATHS (used by PositionController)
    List<Position> findByIdDistributorId(Long distributorId);

    List<Position> findByIdFundId(Long fundId);


    @Query("select distinct p.id.fundId from Position p")
    List<Long> findDistinctFundIds();

}
