package com.example.valuation.controller;

import com.example.valuation.dto.PositionResponse;
import com.example.valuation.entity.*;
import com.example.valuation.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/positions")
public class PositionController {

    private final PositionService positionService;

    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }


    @GetMapping("/all")
    public List<PositionResponse> getAllPositions() {
        return positionService.getAllPositions();
    }
    

    @GetMapping("/{distributorId}/{fundId}")
    public ResponseEntity<PositionResponse> getPosition(
            @PathVariable Long distributorId,
            @PathVariable Long fundId) {

        return ResponseEntity.ok(
                positionService.getPosition(distributorId, fundId));
    }

    @GetMapping("/distributor/{distributorId}")
    public ResponseEntity<List<PositionResponse>> getDistributorPositions(
            @PathVariable Long distributorId) {

        return ResponseEntity.ok(
                positionService.getDistributorPositions(distributorId));
    }

    @GetMapping("/fund/{fundId}")
    public ResponseEntity<List<PositionResponse>> getFundPositions(
            @PathVariable Long fundId) {

        return ResponseEntity.ok(
                positionService.getFundPositions(fundId));
    }
}
