package org.sii.siiassignment.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.sii.siiassignment.exception.ExchangeRateException;
import org.sii.siiassignment.exception.InvalidCurrencyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private static final long CACHE_DURATION = 3600000; // 1 hour
    private final WebClient webClient;
    private final Map<String, BigDecimal> ratesCache = new ConcurrentHashMap<>();
    private long lastUpdateTime = 0;
    @Value("${exchange.rate.api.key:}")
    private String apiKey;

    public Map<String, BigDecimal> getRatesCache() {
        if (shouldUpdateCache()) {
            updateRates();
        }
        return ratesCache;
    }

    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        if (!isCurrencySupported(fromCurrency)) {
            throw new InvalidCurrencyException("Unsupported or invalid currency code: " + fromCurrency);
        }
        if (!isCurrencySupported(toCurrency)) {
            throw new InvalidCurrencyException("Unsupported or invalid currency code: " + toCurrency);
        }

        if (fromCurrency.equals(toCurrency)) {
            return BigDecimal.ONE;
        }

        BigDecimal fromRate = ratesCache.get(fromCurrency);
        BigDecimal toRate = ratesCache.get(toCurrency);

        return toRate.divide(fromRate, 6, RoundingMode.HALF_UP);
    }

    public boolean isCurrencySupported(String currencyCode) {
        if (!isValidCurrencyFormat(currencyCode)) {
            return false;
        }

        if (shouldUpdateCache()) {
            updateRates();
        }

        return ratesCache.containsKey(currencyCode);
    }

    private boolean isValidCurrencyFormat(String currencyCode) {
        return currencyCode != null && currencyCode.matches("[A-Z]{3}");
    }

    private boolean shouldUpdateCache() {
        return System.currentTimeMillis() - lastUpdateTime > CACHE_DURATION || ratesCache.isEmpty();
    }

    private void updateRates() {
        try {
            ExchangeRateResponse response = webClient.get()
                    .uri((apiKey.isEmpty() ? "" : "/" + apiKey) + "/latest/EUR")
                    .retrieve()
                    .bodyToMono(ExchangeRateResponse.class)
                    .block();

            if (response != null && response.rates != null) {
                ratesCache.clear();
                ratesCache.putAll(response.rates);
                lastUpdateTime = System.currentTimeMillis();
            } else {
                throw new ExchangeRateException("Failed to fetch exchange rates: empty response");
            }
        } catch (WebClientException e) {
            throw new ExchangeRateException("Failed to fetch exchange rates: " + e.getMessage());
        }
    }

    @Data
    private static class ExchangeRateResponse {
        @JsonProperty("conversion_rates")
        private Map<String, BigDecimal> rates;
    }
}