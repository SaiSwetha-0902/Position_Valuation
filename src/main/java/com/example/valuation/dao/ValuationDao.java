package com.example.valuation.dao;

import com.example.valuation.entity.ValuationEntity;
import com.example.valuation.entity.ValuationStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ValuationDao extends JpaRepository<ValuationEntity, UUID> {
    long count();
    List<ValuationEntity> findByValuationDate(LocalDate valuationDate);
	List<ValuationEntity> findByStatusAndValuationDate(ValuationStatus status, LocalDate today);

}
