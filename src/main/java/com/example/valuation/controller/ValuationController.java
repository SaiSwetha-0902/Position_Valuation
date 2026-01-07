package com.example.valuation.controller;


import com.example.valuation.dto.CanonicalTradeDTO;
import com.example.valuation.entity.ValuationEntity;
import com.example.valuation.service.ValuationService;
import com.example.valuation.service.StatusTrackingService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/valuation")
public class ValuationController {

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
    
    // GET mapping

}
