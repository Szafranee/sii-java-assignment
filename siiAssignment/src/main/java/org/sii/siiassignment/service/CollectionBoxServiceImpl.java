package org.sii.siiassignment.service;

import lombok.RequiredArgsConstructor;
import org.sii.siiassignment.CollectionBox;
import org.sii.siiassignment.Currency;
import org.sii.siiassignment.DTO.CollectionBox.CollectionBoxResponse;
import org.sii.siiassignment.DTO.CollectionBox.CollectionBoxSummaryResponse;
import org.sii.siiassignment.DTO.CollectionBox.DepositMoneyRequest;
import org.sii.siiassignment.FundraisingEvent;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollectionBoxServiceImpl implements CollectionBoxService {

    // For simplicity, hardcoded exchange rates relative to a base currency - EUR
    // EUR is 1.0 (base)
    // USD to EUR: 0.92 (1 USD = 0.92 EUR)
    // PLN to EUR: 0.23 (1 PLN = 0.23 EUR)
    // GBP to EUR: 1.17 (1 GBP = 1.17 EUR)
    private static final Map<Currency, BigDecimal> EXCHANGE_RATES_TO_EUR = Map.of(
            Currency.EUR, BigDecimal.ONE,
            Currency.USD, new BigDecimal("0.92"),
            Currency.PLN, new BigDecimal("0.23"),
            Currency.GBP, new BigDecimal("1.17")
    );
    private static final EnumSet<Currency> ALLOWED_CURRENCIES = EnumSet.of(Currency.EUR, Currency.USD, Currency.PLN, Currency.GBP);
    private final CollectionBoxRepository collectionBoxRepository;
    private final FundraisingEventRepository fundraisingEventRepository;

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

        // Dla metod walidacyjnych:
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be positive.");
        }

        Currency currency;
        try {
            currency = request.getCurrency();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid currency code provided: " + request.getCurrency(), e);
        }

        if (!ALLOWED_CURRENCIES.contains(currency)) {
            throw new InvalidCurrencyException("Currency " + currency + " is not supported for collection boxes.");
        }

        BigDecimal currentAmount = box.getAmounts().getOrDefault(currency, BigDecimal.ZERO);
        box.getAmounts().put(currency, currentAmount.add(request.getAmount()));

        CollectionBox savedBox = collectionBoxRepository.save(box);
        return mapToCollectionBoxResponse(savedBox);
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
        Currency eventCurrency = event.getAccountCurrency();

        for (Map.Entry<Currency, BigDecimal> entry : box.getAmounts().entrySet()) {
            Currency boxCurrencyToConvert;
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

    private BigDecimal convertCurrency(BigDecimal amount, Currency fromCurrency, Currency toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }

        BigDecimal fromRate = EXCHANGE_RATES_TO_EUR.get(fromCurrency);
        BigDecimal toRate = EXCHANGE_RATES_TO_EUR.get(toCurrency);

        if (fromRate == null || fromRate.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Exchange rate not available for currency: " + fromCurrency);
        }
        if (toRate == null || toRate.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Exchange rate not available for currency: " + toCurrency);
        }

        BigDecimal amountInEur = amount.multiply(fromRate);
        return amountInEur.divide(toRate, 2, RoundingMode.HALF_UP);
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