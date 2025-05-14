package org.sii.siiassignment.service;

import lombok.RequiredArgsConstructor;
import org.sii.siiassignment.DTO.FundraisingEvent.CreateFundraisingEventRequest;
import org.sii.siiassignment.DTO.FundraisingEvent.FinancialReportEntry;
import org.sii.siiassignment.DTO.FundraisingEvent.FundraisingEventResponse;
import org.sii.siiassignment.FundraisingEvent;
import org.sii.siiassignment.exception.InvalidCurrencyException;
import org.sii.siiassignment.exception.ResourceNotFoundException;
import org.sii.siiassignment.repository.FundraisingEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FundraisingEventServiceImpl implements FundraisingEventService {

    private final FundraisingEventRepository fundraisingEventRepository;
    private final ExchangeRateService exchangeRateService;

    @Override
    @Transactional
    public FundraisingEventResponse createFundraisingEvent(CreateFundraisingEventRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Fundraising event name cannot be empty.");
        }
        if (request.getAccountCurrency() == null) {
            throw new IllegalArgumentException("Fundraising event account currency must be specified.");
        }
        if (!validateCurrency(request.getAccountCurrency())) {
            throw new InvalidCurrencyException("Currency " + request.getAccountCurrency() + " is not supported for collection boxes. " +
                    "Might be a non-existent currency, use ISO 4217 code.");
        }

        FundraisingEvent fundraisingEvent = new FundraisingEvent();
        fundraisingEvent.setName(request.getName());
        fundraisingEvent.setAccountCurrency(request.getAccountCurrency());
        fundraisingEvent.setAccountBalance(BigDecimal.ZERO);

        FundraisingEvent savedEvent = fundraisingEventRepository.save(fundraisingEvent);

        return mapToFundraisingEventResponse(savedEvent);
    }

    @Override
    @Transactional
    public FundraisingEventResponse getFundraisingEventById(UUID id) {
        return fundraisingEventRepository.findById(id)
                .map(this::mapToFundraisingEventResponse)
                .orElseThrow(() -> new ResourceNotFoundException("FundraisingEvent not found with id: " + id));
    }

    @Override
    @Transactional
    public List<FinancialReportEntry> getFinancialReport() {
        return fundraisingEventRepository.findAll().stream()
                .map(this::mapToFinancialReportEntry)
                .collect(Collectors.toList());
    }

    private FundraisingEventResponse mapToFundraisingEventResponse(FundraisingEvent event) {
        if (event == null) return null;
        return new FundraisingEventResponse(
                event.getId(),
                event.getName(),
                event.getAccountCurrency(),
                event.getAccountBalance()
        );
    }

    private FinancialReportEntry mapToFinancialReportEntry(FundraisingEvent event) {
        if (event == null) return null;
        return new FinancialReportEntry(
                event.getName(),
                event.getAccountBalance(),
                event.getAccountCurrency()
        );
    }

    private boolean validateCurrency(String currency) {
        Set<String> availableCurrencies = exchangeRateService.getRatesCache().keySet();
        if (!availableCurrencies.contains(currency)) {
            throw new InvalidCurrencyException("Currency " + currency + " is not supported for collection boxes. " +
                    "Might be a non-existent currency, use ISO 4217 code.");
        }
        return true;
    }
}