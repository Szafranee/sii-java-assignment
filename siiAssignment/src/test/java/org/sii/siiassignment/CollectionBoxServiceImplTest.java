package org.sii.siiassignment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sii.siiassignment.DTO.CollectionBox.CollectionBoxResponse;
import org.sii.siiassignment.DTO.CollectionBox.CollectionBoxSummaryResponse;
import org.sii.siiassignment.DTO.CollectionBox.DepositMoneyRequest;
import org.sii.siiassignment.exception.CollectionBoxStateException;
import org.sii.siiassignment.exception.InvalidAmountException;
import org.sii.siiassignment.exception.InvalidCurrencyException;
import org.sii.siiassignment.model.CollectionBox;
import org.sii.siiassignment.model.FundraisingEvent;
import org.sii.siiassignment.repository.CollectionBoxRepository;
import org.sii.siiassignment.repository.FundraisingEventRepository;
import org.sii.siiassignment.service.CollectionBoxServiceImpl;
import org.sii.siiassignment.service.ExchangeRateService;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollectionBoxServiceImplTest {

    @Mock
    private CollectionBoxRepository collectionBoxRepository;

    @Mock
    private FundraisingEventRepository fundraisingEventRepository;

    @Mock
    private ExchangeRateService exchangeRateService;

    @InjectMocks
    private CollectionBoxServiceImpl collectionBoxService;

    private UUID boxId;
    private UUID eventId;
    private CollectionBox collectionBox;
    private FundraisingEvent fundraisingEvent;

    @BeforeEach
    void setUp() {
        boxId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        collectionBox = new CollectionBox();
        collectionBox.setId(boxId);
        collectionBox.setAmounts(new HashMap<>());

        fundraisingEvent = new FundraisingEvent();
        fundraisingEvent.setId(eventId);
        fundraisingEvent.setAccountCurrency("EUR");
        fundraisingEvent.setAccountBalance(BigDecimal.ZERO);
    }

    @Nested
    @DisplayName("Register Collection Box Tests")
    class RegisterCollectionBoxTests {

        @Test
        @DisplayName("Should successfully register new collection box")
        void shouldRegisterNewCollectionBox() {
            // Given
            when(collectionBoxRepository.save(any(CollectionBox.class)))
                    .thenReturn(collectionBox);

            // When
            CollectionBoxResponse response = collectionBoxService.registerCollectionBox();

            // Then
            assertNotNull(response);
            assertTrue(response.isEmpty());
            assertFalse(response.isAssigned());
            verify(collectionBoxRepository).save(any(CollectionBox.class));
        }
    }

    @Nested
    @DisplayName("List Collection Boxes Tests")
    class ListCollectionBoxesTests {

        @Test
        @DisplayName("Should return empty list when no boxes exist")
        void shouldReturnEmptyListWhenNoBoxes() {
            // Given
            when(collectionBoxRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            List<CollectionBoxSummaryResponse> result = collectionBoxService.listAllCollectionBoxes();

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should return list of all collection boxes")
        void shouldReturnListOfAllBoxes() {
            // Given
            List<CollectionBox> boxes = Arrays.asList(collectionBox, new CollectionBox());
            when(collectionBoxRepository.findAll()).thenReturn(boxes);

            // When
            List<CollectionBoxSummaryResponse> result = collectionBoxService.listAllCollectionBoxes();

            // Then
            assertEquals(2, result.size());
        }
    }

    @Nested
    @DisplayName("Assign Collection Box Tests")
    class AssignCollectionBoxTests {

        @Test
        @DisplayName("Should successfully assign empty box to event")
        void shouldAssignEmptyBoxToEvent() {
            // Given
            when(collectionBoxRepository.findById(boxId)).thenReturn(Optional.of(collectionBox));
            when(fundraisingEventRepository.findById(eventId)).thenReturn(Optional.of(fundraisingEvent));
            when(collectionBoxRepository.save(any(CollectionBox.class))).thenReturn(collectionBox);

            // When
            CollectionBoxResponse response = collectionBoxService.assignCollectionBoxToEvent(boxId, eventId);

            // Then
            assertNotNull(response);
            assertEquals(eventId, response.getFundraisingEventId());
            verify(collectionBoxRepository).save(any(CollectionBox.class));
        }

        @Test
        @DisplayName("Should throw exception when assigning non-empty box")
        void shouldThrowExceptionWhenAssigningNonEmptyBox() {
            // Given
            collectionBox.getAmounts().put("EUR", BigDecimal.TEN);
            when(collectionBoxRepository.findById(boxId)).thenReturn(Optional.of(collectionBox));

            // When & Then
            assertThrows(CollectionBoxStateException.class,
                    () -> collectionBoxService.assignCollectionBoxToEvent(boxId, eventId));
        }
    }

    @Nested
    @DisplayName("Deposit Money Tests")
    class DepositMoneyTests {
        @Test
        @DisplayName("Should successfully deposit valid amount")
        void shouldDepositValidAmount() {
            // Given
            DepositMoneyRequest request = new DepositMoneyRequest("EUR", new BigDecimal("100.00"));
            when(collectionBoxRepository.findById(boxId)).thenReturn(Optional.of(collectionBox));
            when(collectionBoxRepository.save(any(CollectionBox.class))).thenReturn(collectionBox);

            Map<String, BigDecimal> ratesMap = new HashMap<>();
            ratesMap.put("EUR", BigDecimal.ONE);
            when(exchangeRateService.getRatesCache()).thenReturn(ratesMap);

            // When
            CollectionBoxResponse response = collectionBoxService.depositMoneyToCollectionBox(boxId, request);

            // Then
            assertNotNull(response);
            assertEquals(new BigDecimal("100.00"), response.getAmounts().get("EUR"));
        }

        @Test
        @DisplayName("Should throw exception for invalid currency")
        void shouldThrowExceptionForInvalidCurrency() {
            // Given
            DepositMoneyRequest request = new DepositMoneyRequest("INVALID", BigDecimal.TEN);
            when(collectionBoxRepository.findById(boxId)).thenReturn(Optional.of(collectionBox));

            // When & Then
            assertThrows(InvalidCurrencyException.class,
                    () -> collectionBoxService.depositMoneyToCollectionBox(boxId, request));
        }

        @Test
        @DisplayName("Should throw exception for negative amount")
        void shouldThrowExceptionForNegativeAmount() {
            // Given
            DepositMoneyRequest request = new DepositMoneyRequest("EUR", new BigDecimal("-10.00"));
            when(collectionBoxRepository.findById(boxId)).thenReturn(Optional.of(collectionBox));

            // When & Then
            assertThrows(InvalidAmountException.class,
                    () -> collectionBoxService.depositMoneyToCollectionBox(boxId, request));
        }
    }

    @Nested
    @DisplayName("Empty Collection Box Tests")
    class EmptyCollectionBoxTests {

        @BeforeEach
        void setUpEmptyTests() {
            collectionBox.setFundraisingEvent(fundraisingEvent);
        }

        @Test
        @DisplayName("Should successfully empty box with single currency")
        void shouldEmptyBoxWithSingleCurrency() {
            // Given
            collectionBox.getAmounts().put("EUR", BigDecimal.TEN); // dodajemy trochę pieniędzy do skarbonki
            when(collectionBoxRepository.findById(boxId)).thenReturn(Optional.of(collectionBox));
            when(collectionBoxRepository.save(any(CollectionBox.class))).thenReturn(collectionBox);
            when(fundraisingEventRepository.save(any(FundraisingEvent.class))).thenReturn(fundraisingEvent);

            // When
            CollectionBoxResponse response = collectionBoxService.emptyCollectionBox(boxId);

            // Then
            assertTrue(response.isEmpty());
            assertEquals(BigDecimal.ZERO, response.getAmounts().getOrDefault("EUR", BigDecimal.ZERO));
            verify(fundraisingEventRepository).save(any(FundraisingEvent.class));
        }

        @Test
        @DisplayName("Should throw exception when emptying unassigned box")
        void shouldThrowExceptionWhenEmptyingUnassignedBox() {
            // Given
            collectionBox.setFundraisingEvent(null);
            when(collectionBoxRepository.findById(boxId)).thenReturn(Optional.of(collectionBox));

            // When & Then
            assertThrows(IllegalStateException.class,
                    () -> collectionBoxService.emptyCollectionBox(boxId));
        }
    }

    @Nested
    @DisplayName("Unregister Collection Box Tests")
    class UnregisterCollectionBoxTests {

        @Test
        @DisplayName("Should successfully unregister box")
        void shouldUnregisterBox() {
            // Given
            when(collectionBoxRepository.findById(boxId)).thenReturn(Optional.of(collectionBox));

            // When
            collectionBoxService.unregisterCollectionBox(boxId);

            // Then
            verify(collectionBoxRepository).delete(collectionBox);
            assertTrue(collectionBox.isEmpty());
        }

        @Test
        @DisplayName("Should throw exception when box not found")
        void shouldThrowExceptionWhenBoxNotFound() {
            // Given
            when(collectionBoxRepository.findById(boxId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(RuntimeException.class,
                    () -> collectionBoxService.unregisterCollectionBox(boxId));
        }
    }
}