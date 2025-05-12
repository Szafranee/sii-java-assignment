package org.sii.siiassignment.service;

import org.sii.siiassignment.DTO.FundraisingEvent.CreateFundraisingEventRequest;
import org.sii.siiassignment.DTO.FundraisingEvent.FinancialReportEntry;
import org.sii.siiassignment.DTO.FundraisingEvent.FundraisingEventResponse;

import java.util.List;
import java.util.UUID;

public interface FundraisingEventService {

    FundraisingEventResponse createFundraisingEvent(CreateFundraisingEventRequest request);

    FundraisingEventResponse getFundraisingEventById(UUID id);

    /**
     * Generates a financial report listing all fundraising events and their balances.
     *
     * @return A list of DTOs, each representing an entry in the financial report.
     */
    List<FinancialReportEntry> getFinancialReport();
}

