package org.sii.siiassignment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sii.siiassignment.DTO.CollectionBox.CollectionBoxResponse;
import org.sii.siiassignment.DTO.CollectionBox.CollectionBoxSummaryResponse;
import org.sii.siiassignment.DTO.CollectionBox.DepositMoneyRequest;
import org.sii.siiassignment.repository.CollectionBoxRepository;
import org.sii.siiassignment.repository.FundraisingEventRepository;
import org.sii.siiassignment.service.CollectionBoxServiceImpl;


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

    @InjectMocks
    private CollectionBoxServiceImpl collectionBoxService; // Zakładamy, że taka klasa istnieje

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
        collectionBox.setAmounts(new HashMap<>()); // Pusta na początku

        fundraisingEvent = new FundraisingEvent();
        fundraisingEvent.setId(eventId);
        fundraisingEvent.setName("Testowe Wydarzenie");
        fundraisingEvent.setAccountCurrency(Currency.PLN);
        fundraisingEvent.setAccountBalance(new BigDecimal("1000.00"));
        fundraisingEvent.setCollectionBoxes(new ArrayList<>()); // Inicjalizacja listy puszek
    }

    @Test
    void registerCollectionBox_powinnoUtworzycIZwrocicNowaPuszke() {
        // Given
        CollectionBox nowaPuszka = new CollectionBox();
        nowaPuszka.setId(UUID.randomUUID()); // Mockito nada ID
        nowaPuszka.setAmounts(new HashMap<>());

        when(collectionBoxRepository.save(any(CollectionBox.class))).thenAnswer(invocation -> {
            CollectionBox cb = invocation.getArgument(0);
            if (cb.getId() == null) cb.setId(UUID.randomUUID()); // Symulacja generowania ID przez bazę
            cb.setAmounts(new HashMap<>()); // Upewniamy się, że amounts jest zainicjowane
            return cb;
        });

        // When
        CollectionBoxResponse response = collectionBoxService.registerCollectionBox();

        // Then
        assertNotNull(response);
        assertNotNull(response.getId());
        assertTrue(response.getAmounts().isEmpty(), "Nowa puszka powinna być pusta.");
        assertNull(response.getFundraisingEventId(), "Nowa puszka nie powinna być przypisana.");
        assertTrue(response.isEmpty(), "Nowa puszka powinna być oznaczona jako pusta.");
        assertFalse(response.isAssigned(), "Nowa puszka nie powinna być oznaczona jako przypisana.");

        verify(collectionBoxRepository, times(1)).save(any(CollectionBox.class));
    }

    @Test
    void listAllCollectionBoxes_powinnoZwrocicListePodsumowan() {
        // Given
        CollectionBox box1 = new CollectionBox();
        box1.setId(UUID.randomUUID());
        box1.setAmounts(Map.of(Currency.PLN, new BigDecimal("10.00")));
        box1.setFundraisingEvent(fundraisingEvent); // Przypisana i niepusta

        CollectionBox box2 = new CollectionBox();
        box2.setId(UUID.randomUUID());
        box2.setAmounts(new HashMap<>()); // Pusta i nieprzypisana

        List<CollectionBox> boxes = List.of(box1, box2);
        when(collectionBoxRepository.findAll()).thenReturn(boxes);

        // When
        List<CollectionBoxSummaryResponse> summaries = collectionBoxService.listAllCollectionBoxes();

        // Then
        assertNotNull(summaries);
        assertEquals(2, summaries.size());

        CollectionBoxSummaryResponse summary1 = summaries.stream().filter(s -> s.getId().equals(box1.getId())).findFirst().orElseThrow();
        assertTrue(summary1.isAssigned(), "Puszka 1 powinna być przypisana");
        assertFalse(summary1.isEmpty(), "Puszka 1 nie powinna być pusta");

        CollectionBoxSummaryResponse summary2 = summaries.stream().filter(s -> s.getId().equals(box2.getId())).findFirst().orElseThrow();
        assertFalse(summary2.isAssigned(), "Puszka 2 nie powinna być przypisana");
        assertTrue(summary2.isEmpty(), "Puszka 2 powinna być pusta");

        verify(collectionBoxRepository, times(1)).findAll();
    }

    @Test
    void unregisterCollectionBox_powinnoUsunacPuszke_gdyIstnieje() {
        // Given
        when(collectionBoxRepository.findById(boxId)).thenReturn(Optional.of(collectionBox));
        doNothing().when(collectionBoxRepository).delete(collectionBox);

        // When
        assertDoesNotThrow(() -> collectionBoxService.unregisterCollectionBox(boxId));

        // Then
        verify(collectionBoxRepository, times(1)).findById(boxId);
        verify(collectionBoxRepository, times(1)).delete(collectionBox);
    }
    
    @Test
    void unregisterCollectionBox_powinnoOdpiacPuszkeOdWydarzeniaPrzedUsunieciem_gdyBylaPrzypisana() {
        // Given
        collectionBox.setFundraisingEvent(fundraisingEvent);
        fundraisingEvent.getCollectionBoxes().add(collectionBox); // Dwustronna relacja

        when(collectionBoxRepository.findById(boxId)).thenReturn(Optional.of(collectionBox));
        doNothing().when(collectionBoxRepository).delete(collectionBox);
        // Nie potrzebujemy mockować save dla fundraisingEvent, bo odpięcie jest zarządzane przez JPA
        // lub powinno być jawnie obsłużone w serwisie jeśli nie ma kaskady

        // When
        assertDoesNotThrow(() -> collectionBoxService.unregisterCollectionBox(boxId));

        // Then
        verify(collectionBoxRepository, times(1)).findById(boxId);
        assertNull(collectionBox.getFundraisingEvent(), "Puszka powinna zostać odpięta od wydarzenia.");
        assertFalse(fundraisingEvent.getCollectionBoxes().contains(collectionBox), "Wydarzenie nie powinno już zawierać tej puszki.");
        verify(collectionBoxRepository, times(1)).delete(collectionBox);
    }


    @Test
    void unregisterCollectionBox_powinnoRzucicWyjatek_gdyPuszkaNieIstnieje() {
        // Given
        when(collectionBoxRepository.findById(boxId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(CollectionBoxNotFoundException.class, () -> {
            collectionBoxService.unregisterCollectionBox(boxId);
        });
        verify(collectionBoxRepository, times(1)).findById(boxId);
        verify(collectionBoxRepository, never()).delete(any());
    }

    @Test
    void assignCollectionBoxToEvent_powinnoPrzypisacPustaPuszkeDoWydarzenia() {
        // Given
        assertTrue(collectionBox.getAmounts().isEmpty(), "Puszka powinna być pusta na starcie testu.");
        collectionBox.setFundraisingEvent(null); // Upewnij się, że nie jest przypisana

        when(collectionBoxRepository.findById(boxId)).thenReturn(Optional.of(collectionBox));
        when(fundraisingEventRepository.findById(eventId)).thenReturn(Optional.of(fundraisingEvent));
        when(collectionBoxRepository.save(any(CollectionBox.class))).thenReturn(collectionBox); // Zapisana puszka

        // When
        CollectionBoxResponse response = collectionBoxService.assignCollectionBoxToEvent(boxId, eventId);

        // Then
        assertNotNull(response);
        assertEquals(boxId, response.getId());
        assertEquals(eventId, response.getFundraisingEventId(), "ID wydarzenia w odpowiedzi powinno się zgadzać.");
        assertNotNull(collectionBox.getFundraisingEvent(), "Puszka powinna mieć przypisane wydarzenie.");
        assertEquals(eventId, collectionBox.getFundraisingEvent().getId(), "Przypisane wydarzenie powinno mieć poprawne ID.");
        assertTrue(fundraisingEvent.getCollectionBoxes().contains(collectionBox), "Wydarzenie powinno zawierać przypisaną puszkę.");
        assertTrue(response.isAssigned());
        assertTrue(response.isEmpty());


        verify(collectionBoxRepository, times(1)).findById(boxId);
        verify(fundraisingEventRepository, times(1)).findById(eventId);
        verify(collectionBoxRepository, times(1)).save(collectionBox);
    }

    @Test
    void assignCollectionBoxToEvent_powinnoRzucicWyjatek_gdyPuszkaNieIstnieje() {
        // Given
        when(collectionBoxRepository.findById(boxId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(CollectionBoxNotFoundException.class, () -> {
            collectionBoxService.assignCollectionBoxToEvent(boxId, eventId);
        });
        verify(collectionBoxRepository, times(1)).findById(boxId);
        verify(fundraisingEventRepository, never()).findById(any());
        verify(collectionBoxRepository, never()).save(any());
    }

    @Test
    void assignCollectionBoxToEvent_powinnoRzucicWyjatek_gdyWydarzenieNieIstnieje() {
        // Given
        when(collectionBoxRepository.findById(boxId)).thenReturn(Optional.of(collectionBox));
        when(fundraisingEventRepository.findById(eventId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(FundraisingEventNotFoundException.class, () -> {
            collectionBoxService.assignCollectionBoxToEvent(boxId, eventId);
        });
        verify(collectionBoxRepository, times(1)).findById(boxId);
        verify(fundraisingEventRepository, times(1)).findById(eventId);
        verify(collectionBoxRepository, never()).save(any());
    }

    @Test
    void assignCollectionBoxToEvent_powinnoRzucicWyjatek_gdyPuszkaNieJestPusta() {
        // Given
        collectionBox.getAmounts().put(Currency.EUR, new BigDecimal("10.00")); // Puszka nie jest pusta
        when(collectionBoxRepository.findById(boxId)).thenReturn(Optional.of(collectionBox));
        when(fundraisingEventRepository.findById(eventId)).thenReturn(Optional.of(fundraisingEvent));

        // When & Then
        assertThrows(CollectionBoxNotEmptyException.class, () -> {
            collectionBoxService.assignCollectionBoxToEvent(boxId, eventId);
        });
        verify(collectionBoxRepository, times(1)).findById(boxId);
        verify(fundraisingEventRepository, times(1)).findById(eventId); // Może być wywołane przed sprawdzeniem czy pusta
        verify(collectionBoxRepository, never()).save(any());
    }
    
    @Test
    void assignCollectionBoxToEvent_powinnoPoprawnieOdpiacOdStaregoWydarzenia() {
        // Given
        FundraisingEvent stareWydarzenie = new FundraisingEvent();
        stareWydarzenie.setId(UUID.randomUUID());
        stareWydarzenie.setName("Stare Wydarzenie");
        stareWydarzenie.setCollectionBoxes(new ArrayList<>(List.of(collectionBox))); // Dodajemy puszkę do starego wydarzenia
        collectionBox.setFundraisingEvent(stareWydarzenie); // Puszka jest przypisana do starego wydarzenia
        collectionBox.setAmounts(new HashMap<>()); // Puszka jest pusta

        when(collectionBoxRepository.findById(boxId)).thenReturn(Optional.of(collectionBox));
        when(fundraisingEventRepository.findById(eventId)).thenReturn(Optional.of(fundraisingEvent)); // Nowe wydarzenie
        when(collectionBoxRepository.save(collectionBox)).thenReturn(collectionBox);

        // When
        collectionBoxService.assignCollectionBoxToEvent(boxId, eventId);

        // Then
        assertFalse(stareWydarzenie.getCollectionBoxes().contains(collectionBox), "Puszka powinna być usunięta ze starego wydarzenia.");
        assertEquals(fundraisingEvent, collectionBox.getFundraisingEvent(), "Puszka powinna być przypisana do nowego wydarzenia.");
        assertTrue(fundraisingEvent.getCollectionBoxes().contains(collectionBox), "Nowe wydarzenie powinno zawierać puszkę.");
        verify(collectionBoxRepository, times(1)).save(collectionBox);
    }


    @Test
    void depositMoneyToCollectionBox_powinnoDodacSrodkiDoPuszki() {
        // Given
        DepositMoneyRequest request = new DepositMoneyRequest(Currency.USD, new BigDecimal("50.00"));
        when(collectionBoxRepository.findById(boxId)).thenReturn(Optional.of(collectionBox));
        when(collectionBoxRepository.save(any(CollectionBox.class))).thenReturn(collectionBox);

        // When
        CollectionBoxResponse response = collectionBoxService.depositMoneyToCollectionBox(boxId, request);

        // Then
        assertNotNull(response);
        assertEquals(new BigDecimal("50.00"), response.getAmounts().get(Currency.USD));
        assertFalse(response.isEmpty());

        verify(collectionBoxRepository, times(1)).findById(boxId);
        verify(collectionBoxRepository, times(1)).save(collectionBox);
    }
    
    @Test
    void depositMoneyToCollectionBox_powinnoZaktualizowacIstniejaceSrodkiWPuszce() {
        // Given
        collectionBox.getAmounts().put(Currency.USD, new BigDecimal("20.00")); // Już są środki w USD
        DepositMoneyRequest request = new DepositMoneyRequest(Currency.USD, new BigDecimal("30.00"));
        when(collectionBoxRepository.findById(boxId)).thenReturn(Optional.of(collectionBox));
        when(collectionBoxRepository.save(any(CollectionBox.class))).thenReturn(collectionBox);

        // When
        CollectionBoxResponse response = collectionBoxService.depositMoneyToCollectionBox(boxId, request);

        // Then
        assertNotNull(response);
        // Oczekujemy 20 + 30 = 50
        assertEquals(0, new BigDecimal("50.00").compareTo(response.getAmounts().get(Currency.USD)), "Kwota w USD powinna zostać zsumowana.");
        assertFalse(response.isEmpty());

        verify(collectionBoxRepository, times(1)).findById(boxId);
        verify(collectionBoxRepository, times(1)).save(collectionBox);
    }


    @Test
    void depositMoneyToCollectionBox_powinnoRzucicWyjatek_gdyKwotaJestUjemna() {
        // Given
        DepositMoneyRequest request = new DepositMoneyRequest(Currency.USD, new BigDecimal("-50.00"));
        when(collectionBoxRepository.findById(boxId)).thenReturn(Optional.of(collectionBox));

        // When & Then
        assertThrows(InvalidAmountException.class, () -> {
            collectionBoxService.depositMoneyToCollectionBox(boxId, request);
        });
        verify(collectionBoxRepository, times(1)).findById(boxId);
        verify(collectionBoxRepository, never()).save(any());
    }

    @Test
    void depositMoneyToCollectionBox_powinnoRzucicWyjatek_gdyPuszkaNieIstnieje() {
        // Given
        DepositMoneyRequest request = new DepositMoneyRequest(Currency.PLN, new BigDecimal("10.00"));
        when(collectionBoxRepository.findById(boxId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(CollectionBoxNotFoundException.class, () -> {
             collectionBoxService.depositMoneyToCollectionBox(boxId, request);
        });
        verify(collectionBoxRepository, never()).save(any());
    }


    @Test
    void emptyCollectionBox_powinnoPrzeniescSrodkiIOproznicPuszke_gdyPrzypisana() {
        // Given
        collectionBox.setFundraisingEvent(fundraisingEvent); // Puszka jest przypisana
        fundraisingEvent.getCollectionBoxes().add(collectionBox);
        collectionBox.getAmounts().put(Currency.EUR, new BigDecimal("100.00")); // 100 EUR
        collectionBox.getAmounts().put(Currency.USD, new BigDecimal("50.00"));  // 50 USD
        // Załóżmy, że event ma walutę PLN, a kursy są: 1 EUR = 4.3 PLN, 1 USD = 4.0 PLN
        // Spodziewany transfer: 100 * 4.3 + 50 * 4.0 = 430 + 200 = 630 PLN
        BigDecimal expectedTransferPln = new BigDecimal("630.00");
        BigDecimal initialEventBalance = fundraisingEvent.getAccountBalance(); // np. 1000 PLN

        when(collectionBoxRepository.findById(boxId)).thenReturn(Optional.of(collectionBox));
        // Mockowanie repozytoriów do zapisu
        when(collectionBoxRepository.save(any(CollectionBox.class))).thenReturn(collectionBox);
        when(fundraisingEventRepository.save(any(FundraisingEvent.class))).thenReturn(fundraisingEvent);

        // When
        CollectionBoxResponse response = collectionBoxService.emptyCollectionBox(boxId);

        // Then
        assertNotNull(response);
        assertTrue(response.getAmounts().isEmpty(), "Puszka powinna być pusta po opróżnieniu.");
        assertTrue(response.isEmpty(), "Puszka powinna być oznaczona jako pusta.");
        assertNotNull(response.getFundraisingEventId(), "Puszka powinna pozostać przypisana.");

        ArgumentCaptor<FundraisingEvent> eventCaptor = ArgumentCaptor.forClass(FundraisingEvent.class);
        verify(fundraisingEventRepository, times(1)).save(eventCaptor.capture());
        FundraisingEvent savedEvent = eventCaptor.getValue();

        assertEquals(0, initialEventBalance.add(expectedTransferPln).compareTo(savedEvent.getAccountBalance()),
                "Saldo wydarzenia powinno zostać poprawnie zaktualizowane.");

        verify(collectionBoxRepository, times(1)).save(collectionBox);
    }

    @Test
    void emptyCollectionBox_powinnoRzucicWyjatek_gdyPuszkaNieJestPrzypisana() {
        // Given
        collectionBox.setFundraisingEvent(null); // Puszka nie jest przypisana
        collectionBox.getAmounts().put(Currency.PLN, new BigDecimal("10.00"));
        when(collectionBoxRepository.findById(boxId)).thenReturn(Optional.of(collectionBox));

        // When & Then
        // Załóżmy IllegalStateException lub dedykowany wyjątek
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            collectionBoxService.emptyCollectionBox(boxId);
        });
        assertEquals("Puszka o ID: " + boxId + " nie jest przypisana do żadnego wydarzenia i nie może zostać opróżniona.", exception.getMessage());
        
        verify(collectionBoxRepository, times(1)).findById(boxId);
        verify(collectionBoxRepository, never()).save(any());
        verify(fundraisingEventRepository, never()).save(any());
    }

    @Test
    void emptyCollectionBox_powinnoRzucicWyjatek_gdyPuszkaNieIstnieje() {
        // Given
        when(collectionBoxRepository.findById(boxId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(CollectionBoxNotFoundException.class, () -> {
            collectionBoxService.emptyCollectionBox(boxId);
        });
        verify(collectionBoxRepository, never()).save(any());
        verify(fundraisingEventRepository, never()).save(any());
    }
    
    @Test
    void emptyCollectionBox_niePowinnoZmieniacSaldaWydarzenia_gdyPuszkaJestPusta() {
        // Given
        collectionBox.setFundraisingEvent(fundraisingEvent);
        fundraisingEvent.getCollectionBoxes().add(collectionBox);
        assertTrue(collectionBox.getAmounts().isEmpty(), "Puszka testowa powinna być pusta.");
        BigDecimal initialEventBalance = fundraisingEvent.getAccountBalance();

        when(collectionBoxRepository.findById(boxId)).thenReturn(Optional.of(collectionBox));
        when(collectionBoxRepository.save(collectionBox)).thenReturn(collectionBox);
        // fundraisingEventRepository.save() nie powinno być wywołane jeśli nic się nie zmienia,
        // ale jeśli jest wołane bezwarunkowo, to saldo nie powinno się zmienić.

        // When
        CollectionBoxResponse response = collectionBoxService.emptyCollectionBox(boxId);

        // Then
        assertNotNull(response);
        assertTrue(response.getAmounts().isEmpty());
        assertTrue(response.isEmpty());

        // Sprawdzamy czy fundraisingEventRepository.save było wywołane (może być lub nie, zależnie od implementacji)
        // Najważniejsze to sprawdzić saldo, jeśli save było wywołane.
        ArgumentCaptor<FundraisingEvent> eventCaptor = ArgumentCaptor.forClass(FundraisingEvent.class);
        // Używamy atMostOnce(), bo jeśli puszka jest pusta, save na wydarzeniu może nie być potrzebny
        verify(fundraisingEventRepository, atMostOnce()).save(eventCaptor.capture()); 

        if (!eventCaptor.getAllValues().isEmpty()) {
            assertEquals(0, initialEventBalance.compareTo(eventCaptor.getValue().getAccountBalance()),
                "Saldo wydarzenia nie powinno się zmienić, gdy opróżniana jest pusta puszka.");
        } else {
            // Jeśli save nie zostało wywołane, to saldo na pewno się nie zmieniło.
            // Można by ewentualnie pobrać event i sprawdzić, ale to już nadmiarowe dla tego testu.
            assertEquals(0, initialEventBalance.compareTo(fundraisingEvent.getAccountBalance()));
        }
        verify(collectionBoxRepository, times(1)).save(collectionBox);
    }
}