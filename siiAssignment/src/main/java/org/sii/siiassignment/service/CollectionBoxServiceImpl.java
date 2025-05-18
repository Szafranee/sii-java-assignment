package org.sii.siiassignment.service;

import lombok.RequiredArgsConstructor;
import org.sii.siiassignment.model.CollectionBox;
import org.sii.siiassignment.DTO.CollectionBox.CollectionBoxResponse;
import org.sii.siiassignment.DTO.CollectionBox.CollectionBoxSummaryResponse;
import org.sii.siiassignment.DTO.CollectionBox.DepositMoneyRequest;
import org.sii.siiassignment.model.FundraisingEvent;
import org.sii.siiassignment.exception.CollectionBoxStateException;
import org.sii.siiassignment.exception.InvalidAmountException;
import org.sii.siiassignment.exception.InvalidCurrencyException;
import org.sii.siiassignment.exception.ResourceNotFoundException;
import org.sii.siiassignment.repository.CollectionBoxRepository;
import org.sii.siiassignment.repository.FundraisingEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollectionBoxServiceImpl implements CollectionBoxService {
    private final CollectionBoxRepository collectionBoxRepository;
    private final FundraisingEventRepository fundraisingEventRepository;
    private final ExchangeRateService exchangeRateService;

    @Override
    @Transactional
    public CollectionBoxResponse registerCollectionBox() {
        CollectionBox newBox = new CollectionBox();
        CollectionBox savedBox = collectionBoxRepository.save(newBox);
        return mapToCollectionBoxResponse(savedBox);
    }

    @Override
    @Transactional
    public List<CollectionBoxSummaryResponse> listAllCollectionBoxes() {
        return collectionBoxRepository.findAll().stream()
                .map(this::mapToCollectionBoxSummaryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CollectionBoxResponse getCollectionBoxById(UUID id) {
        return collectionBoxRepository.findById(id)
                .map(this::mapToCollectionBoxResponse)
                .orElseThrow(() -> new ResourceNotFoundException("CollectionBox not found with id: " + id));
    }

    @Override
    @Transactional
    public void unregisterCollectionBox(UUID boxId) {
        CollectionBox box = collectionBoxRepository.findById(boxId)
                .orElseThrow(() -> new RuntimeException("CollectionBox not found with id: " + boxId));

        box.clearAmounts();
        box.setFundraisingEvent(null);
        collectionBoxRepository.save(box);
        collectionBoxRepository.delete(box);
    }

    @Override
    @Transactional
    public CollectionBoxResponse assignCollectionBoxToEvent(UUID boxId, UUID eventId) {
        CollectionBox collectionBox = collectionBoxRepository.findById(boxId)
                .orElseThrow(() -> new RuntimeException("CollectionBox not found with id: " + boxId));

        if (!collectionBox.isEmpty()) {
            throw new CollectionBoxStateException("Collection box must be empty to be assigned to a fundraising event.");
        }
        if (collectionBox.isAssigned()) {
            if (!collectionBox.getFundraisingEvent().getId().equals(eventId)) {
                throw new IllegalStateException("Collection box is already assigned to a different fundraising event. Unassign first.");
            } else {
                return mapToCollectionBoxResponse(collectionBox);
            }
        }

        FundraisingEvent event = fundraisingEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("FundraisingEvent not found with id: " + eventId));

        collectionBox.setFundraisingEvent(event);
        CollectionBox savedBox = collectionBoxRepository.save(collectionBox);
        return mapToCollectionBoxResponse(savedBox);
    }

    @Override
    @Transactional
    public CollectionBoxResponse depositMoneyToCollectionBox(UUID boxId, DepositMoneyRequest request) {
        CollectionBox box = collectionBoxRepository.findById(boxId)
                .orElseThrow(() -> new RuntimeException("CollectionBox not found with id: " + boxId));

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be positive.");
        }

        String currency = validateCurrency(request);

        BigDecimal currentAmount = box.getAmounts().getOrDefault(currency, BigDecimal.ZERO);
        box.getAmounts().put(currency, currentAmount.add(request.getAmount()));

        CollectionBox savedBox = collectionBoxRepository.save(box);
        return mapToCollectionBoxResponse(savedBox);
    }

    private String validateCurrency(DepositMoneyRequest request) {
        String currency;
        try {
            currency = request.getCurrency();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid currency code provided: " + request.getCurrency(), e);
        }

        Set<String> availableCurrencies = exchangeRateService.getRatesCache().keySet();
        if (!availableCurrencies.contains(currency)) {
            throw new InvalidCurrencyException("Currency " + currency + " is not supported for collection boxes. " +
                    "Might be a non-existent currency, use ISO 4217 code.");
        }
        return currency;
    }

    @Override
    @Transactional
    public CollectionBoxResponse emptyCollectionBox(UUID boxId) {
        CollectionBox box = collectionBoxRepository.findById(boxId)
                .orElseThrow(() -> new RuntimeException("CollectionBox not found with id: " + boxId));

        if (!box.isAssigned()) {
            throw new IllegalStateException("Collection box is not assigned to any fundraising event. Cannot transfer money.");
        }
        if (box.isEmpty()) {
            return mapToCollectionBoxResponse(box);
        }

        FundraisingEvent event = box.getFundraisingEvent();
        if (event == null) {
            throw new IllegalStateException("Consistency error: Box is assigned but FundraisingEvent is null.");
        }

        BigDecimal totalAmountInEventCurrency = BigDecimal.ZERO;
        String eventCurrency = event.getAccountCurrency();

        for (Map.Entry<String, BigDecimal> entry : box.getAmounts().entrySet()) {
            String boxCurrencyToConvert;
            try {
                boxCurrencyToConvert = entry.getKey();
            } catch (IllegalArgumentException e) {
                System.err.println("Skipping unknown currency in box: " + entry.getKey());
                continue;
            }
            BigDecimal amountInBoxCurrencyToConvert = entry.getValue();

            if (amountInBoxCurrencyToConvert == null || amountInBoxCurrencyToConvert.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            BigDecimal convertedAmount = convertCurrency(amountInBoxCurrencyToConvert, boxCurrencyToConvert, eventCurrency);
            totalAmountInEventCurrency = totalAmountInEventCurrency.add(convertedAmount);
        }

        event.setAccountBalance(event.getAccountBalance().add(totalAmountInEventCurrency));
        fundraisingEventRepository.save(event);

        box.clearAmounts();
        CollectionBox savedBox = collectionBoxRepository.save(box);

        return mapToCollectionBoxResponse(savedBox);
    }

    private BigDecimal convertCurrency(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }

        BigDecimal exchangeRate = exchangeRateService.getExchangeRate(fromCurrency, toCurrency);

        return amount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);

    }


    private CollectionBoxResponse mapToCollectionBoxResponse(CollectionBox box) {
        if (box == null) return null;
        return new CollectionBoxResponse(
                box.getId(),
                box.getFundraisingEvent() != null ? box.getFundraisingEvent().getId() : null,
                box.getAmounts(),
                box.isEmpty(),
                box.isAssigned()
        );
    }

    private CollectionBoxSummaryResponse mapToCollectionBoxSummaryResponse(CollectionBox box) {
        if (box == null) return null;
        return new CollectionBoxSummaryResponse(
                box.getId(),
                box.isAssigned(),
                box.isEmpty()
        );
    }
}