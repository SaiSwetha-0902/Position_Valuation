package com.example.valuation.controller;


import com.example.valuation.dto.CanonicalTradeDTO;
import com.example.valuation.entity.ValuationEntity;
import com.example.valuation.service.ValuationService;
import com.example.valuation.service.StatusTrackingService;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/valuation")
public class ValuationController {
	private static final Logger logger = LoggerFactory.getLogger(ValuationService.class);

    @Autowired
    private ValuationService valuationService;

    @Autowired
    private StatusTrackingService statusTrackingService;

    @PostMapping("/process")
    public ResponseEntity<ValuationEntity> process(@RequestBody CanonicalTradeDTO dto) {
	    try {
	        ValuationEntity valuation = valuationService.valuation(dto);
	        //statusTrackingService.trackStatus(dto,null);
	        if(valuation !=null) {
	            
	        }
	        return ResponseEntity.ok(valuation);
	    } catch (Exception e) {
	       // statusTrackingService.trackStatus(dto, e);
	        return ResponseEntity.badRequest().body(null);
	    } 
   }
    
    // Fetch all records
    @GetMapping("/valuations")
    public ResponseEntity<List<ValuationEntity>> getAllValuations() {

        List<ValuationEntity> valuations = valuationService.allValuationRecords();

        if (valuations.isEmpty()) {
        	logger.info("Valuation table is empty!!");
            return ResponseEntity.noContent().build(); // 204 table empty
        }

        logger.info("Found {} records in valuation table..", valuations.size());
        return ResponseEntity.ok(valuations); // 200
    }
    
    // Fetch all records in paginated format
    @GetMapping("/paginated-valuations")
    public Page<ValuationEntity> getValuations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return valuationService.getPaginated(page, size);
    }



}
