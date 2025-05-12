package org.sii.siiassignment.controller;

import lombok.RequiredArgsConstructor;
import org.sii.siiassignment.DTO.FundraisingEvent.CreateFundraisingEventRequest;
import org.sii.siiassignment.DTO.FundraisingEvent.FinancialReportEntry;
import org.sii.siiassignment.DTO.FundraisingEvent.FundraisingEventResponse;
import org.sii.siiassignment.service.FundraisingEventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fundraising-events")
@RequiredArgsConstructor
public class FundraisingEventController {

    private final FundraisingEventService fundraisingEventService;

    /**
     * Endpoint 1: Create a new fundraising event.
     * @param request The request body containing details for the new fundraising event.
     * @return The created fundraising event.
     */
    @PostMapping
    public ResponseEntity<FundraisingEventResponse> createFundraisingEvent(@RequestBody CreateFundraisingEventRequest request) {
        FundraisingEventResponse response = fundraisingEventService.createFundraisingEvent(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Endpoint 8: Display a financial report with all fundraising events and the sum of their accounts.
     * @return A list of financial report entries.
     */
    @GetMapping("/report")
    public ResponseEntity<List<FinancialReportEntry>> getFinancialReport() {
        List<FinancialReportEntry> report = fundraisingEventService.getFinancialReport();
        return ResponseEntity.ok(report);
    }
}
