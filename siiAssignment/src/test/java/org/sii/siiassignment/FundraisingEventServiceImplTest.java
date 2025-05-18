package org.sii.siiassignment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sii.siiassignment.DTO.FundraisingEvent.CreateFundraisingEventRequest;
import org.sii.siiassignment.DTO.FundraisingEvent.FinancialReportEntry;
import org.sii.siiassignment.DTO.FundraisingEvent.FundraisingEventResponse;
import org.sii.siiassignment.exception.InvalidCurrencyException;
import org.sii.siiassignment.exception.ResourceNotFoundException;
import org.sii.siiassignment.model.FundraisingEvent;
import org.sii.siiassignment.repository.FundraisingEventRepository;
import org.sii.siiassignment.service.ExchangeRateService;
import org.sii.siiassignment.service.FundraisingEventServiceImpl;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FundraisingEventServiceImplTest {

    @Mock
    private FundraisingEventRepository fundraisingEventRepository;

    @Mock
    private ExchangeRateService exchangeRateService;

    @InjectMocks
    private FundraisingEventServiceImpl fundraisingEventService;

    private UUID eventId;
    private FundraisingEvent fundraisingEvent;
    private CreateFundraisingEventRequest createRequest;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        fundraisingEvent = new FundraisingEvent();
        fundraisingEvent.setId(eventId);
        fundraisingEvent.setName("Test Event");
        fundraisingEvent.setAccountCurrency("EUR");
        fundraisingEvent.setAccountBalance(BigDecimal.ZERO);

        createRequest = new CreateFundraisingEventRequest(
                "Test Event",
                "EUR"
        );
    }

    @Nested
    @DisplayName("Create Fundraising Event Tests")
    class CreateFundraisingEventTests {


        @Test
        @DisplayName("Should successfully create fundraising event with valid data")
        void shouldCreateFundraisingEventWithValidData() {
            // Given
            Map<String, BigDecimal> ratesCache = new HashMap<>();
            ratesCache.put("EUR", BigDecimal.ONE);
            when(exchangeRateService.getRatesCache()).thenReturn(ratesCache);
            when(fundraisingEventRepository.save(any(FundraisingEvent.class)))
                    .thenReturn(fundraisingEvent);

            // When
            FundraisingEventResponse response = fundraisingEventService.createFundraisingEvent(createRequest);

            // Then
            assertNotNull(response);
            assertEquals("Test Event", response.getName());
            assertEquals("EUR", response.getAccountCurrency());
            assertEquals(BigDecimal.ZERO, response.getAccountBalance());

            ArgumentCaptor<FundraisingEvent> eventCaptor = ArgumentCaptor.forClass(FundraisingEvent.class);
            verify(fundraisingEventRepository).save(eventCaptor.capture());
            FundraisingEvent savedEvent = eventCaptor.getValue();
            assertEquals(createRequest.getName(), savedEvent.getName());
            assertEquals(createRequest.getAccountCurrency(), savedEvent.getAccountCurrency());
        }

        @Test
        @DisplayName("Should throw exception when creating event with invalid currency")
        void shouldThrowExceptionForInvalidCurrency() {
            // Given
            createRequest.setAccountCurrency("INVALID");

            // When & Then
            assertThrows(InvalidCurrencyException.class,
                    () -> fundraisingEventService.createFundraisingEvent(createRequest));
            verify(fundraisingEventRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when creating event with null name")
        void shouldThrowExceptionForNullName() {
            // Given
            createRequest.setName(null);

            // When & Then
            assertThrows(IllegalArgumentException.class,
                    () -> fundraisingEventService.createFundraisingEvent(createRequest));
            verify(fundraisingEventRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when creating event with empty name")
        void shouldThrowExceptionForEmptyName() {
            // Given
            createRequest.setName("");

            // When & Then
            assertThrows(IllegalArgumentException.class,
                    () -> fundraisingEventService.createFundraisingEvent(createRequest));
            verify(fundraisingEventRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Get Fundraising Event Tests")
    class GetFundraisingEventTests {

        @Test
        @DisplayName("Should successfully retrieve existing fundraising event")
        void shouldRetrieveExistingEvent() {
            // Given
            when(fundraisingEventRepository.findById(eventId))
                    .thenReturn(Optional.of(fundraisingEvent));

            // When
            FundraisingEventResponse response = fundraisingEventService.getFundraisingEventById(eventId);

            // Then
            assertNotNull(response);
            assertEquals(eventId, response.getId());
            assertEquals("Test Event", response.getName());
        }

        @Test
        @DisplayName("Should throw exception when event not found")
        void shouldThrowExceptionWhenEventNotFound() {
            // Given
            when(fundraisingEventRepository.findById(eventId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThrows(ResourceNotFoundException.class,
                    () -> fundraisingEventService.getFundraisingEventById(eventId));
        }
    }

    @Nested
    @DisplayName("Financial Report Tests")
    class FinancialReportTests {

        @Test
        @DisplayName("Should return empty report when no events exist")
        void shouldReturnEmptyReportWhenNoEvents() {
            // Given
            when(fundraisingEventRepository.findAll())
                    .thenReturn(Collections.emptyList());

            // When
            List<FinancialReportEntry> report = fundraisingEventService.getFinancialReport();

            // Then
            assertTrue(report.isEmpty());
        }

        @Test
        @DisplayName("Should return correct financial report with multiple events")
        void shouldReturnCorrectFinancialReport() {
            // Given
            FundraisingEvent event1 = new FundraisingEvent();
            event1.setName("Event 1");
            event1.setAccountCurrency("EUR");
            event1.setAccountBalance(new BigDecimal("100.00"));

            FundraisingEvent event2 = new FundraisingEvent();
            event2.setName("Event 2");
            event2.setAccountCurrency("USD");
            event2.setAccountBalance(new BigDecimal("200.00"));

            when(fundraisingEventRepository.findAll())
                    .thenReturn(Arrays.asList(event1, event2));

            // When
            List<FinancialReportEntry> report = fundraisingEventService.getFinancialReport();

            // Then
            assertEquals(2, report.size());

            FinancialReportEntry entry1 = report.getFirst();
            assertEquals("Event 1", entry1.getFundraisingEventName());
            assertEquals(new BigDecimal("100.00"), entry1.getAmount());
            assertEquals("EUR", entry1.getCurrency());

            FinancialReportEntry entry2 = report.get(1);
            assertEquals("Event 2", entry2.getFundraisingEventName());
            assertEquals(new BigDecimal("200.00"), entry2.getAmount());
            assertEquals("USD", entry2.getCurrency());
        }

        @Test
        @DisplayName("Should handle events with zero balance in report")
        void shouldHandleZeroBalanceInReport() {
            // Given
            fundraisingEvent.setAccountBalance(BigDecimal.ZERO);
            when(fundraisingEventRepository.findAll())
                    .thenReturn(Collections.singletonList(fundraisingEvent));

            // When
            List<FinancialReportEntry> report = fundraisingEventService.getFinancialReport();

            // Then
            assertEquals(1, report.size());
            FinancialReportEntry entry = report.getFirst();
            assertEquals(BigDecimal.ZERO, entry.getAmount());
        }
    }
}