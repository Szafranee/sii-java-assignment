
package org.sii.siiassignment.controller;

import lombok.RequiredArgsConstructor;
import org.sii.siiassignment.DTO.CollectionBox.CollectionBoxResponse;
import org.sii.siiassignment.DTO.CollectionBox.CollectionBoxSummaryResponse;
import org.sii.siiassignment.DTO.CollectionBox.DepositMoneyRequest;
import org.sii.siiassignment.service.CollectionBoxService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/collection-boxes")
@RequiredArgsConstructor
public class CollectionBoxController {

    private final CollectionBoxService collectionBoxService;

    /**
     * Endpoint 2: Register a new collection box.
     * @return The registered collection box.
     */
    @PostMapping
    public ResponseEntity<CollectionBoxResponse> registerCollectionBox() {
        CollectionBoxResponse response = collectionBoxService.registerCollectionBox();
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Endpoint 3: List all collection boxes.
     * Includes information if the box is assigned and if it is empty, without exposing sensitive details.
     * @return A list of collection boxes with limited information.
     */
    @GetMapping
    public ResponseEntity<List<CollectionBoxSummaryResponse>> listCollectionBoxes() {
        List<CollectionBoxSummaryResponse> boxes = collectionBoxService.listAllCollectionBoxes();
        return ResponseEntity.ok(boxes);
    }

    /**
     * Endpoint 4: Unregister (remove) a collection box.
     * @param id The ID of the collection box to unregister.
     * @return HTTP 204 No Content on successful deletion.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> unregisterCollectionBox(@PathVariable UUID id) {
        collectionBoxService.unregisterCollectionBox(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint 5: Assign the collection box to an existing fundraising event.
     * @param boxId The ID of the collection box.
     * @param eventId The ID of the fundraising event.
     * @return The updated collection box details.
     */
    @PutMapping("/{boxId}/assign/{eventId}")
    public ResponseEntity<CollectionBoxResponse> assignCollectionBoxToEvent(@PathVariable UUID boxId, @PathVariable UUID eventId) {
        CollectionBoxResponse response = collectionBoxService.assignCollectionBoxToEvent(boxId, eventId);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint 6: Put (add) some money inside the collection box.
     * @param boxId The ID of the collection box.
     * @param request The request body containing the amount and currency of the money to deposit.
     * @return The updated collection box details.
     */
    @PutMapping("/{boxId}/deposit")
    public ResponseEntity<CollectionBoxResponse> depositMoneyToCollectionBox(@PathVariable UUID boxId, @RequestBody DepositMoneyRequest request) {
        CollectionBoxResponse response = collectionBoxService.depositMoneyToCollectionBox(boxId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint 7: Empty the collection box.
     * Transfers money from the box to the fundraising event's account.
     * @param boxId The ID of the collection box to empty.
     * @return The updated collection box details.
     */
    @PutMapping("/{boxId}/empty")
    public ResponseEntity<CollectionBoxResponse> emptyCollectionBox(@PathVariable UUID boxId) {
        CollectionBoxResponse response = collectionBoxService.emptyCollectionBox(boxId);
        return ResponseEntity.ok(response);
    }
}
