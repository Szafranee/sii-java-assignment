package org.sii.siiassignment.service;

import org.sii.siiassignment.DTO.CollectionBox.DepositMoneyRequest;
import org.sii.siiassignment.DTO.CollectionBox.CollectionBoxResponse;
import org.sii.siiassignment.DTO.CollectionBox.CollectionBoxSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface CollectionBoxService {

    CollectionBoxResponse registerCollectionBox();

    List<CollectionBoxSummaryResponse> listAllCollectionBoxes();

    CollectionBoxResponse getCollectionBoxById(UUID id);

    void unregisterCollectionBox(UUID boxId);

    CollectionBoxResponse assignCollectionBoxToEvent(UUID boxId, UUID eventId);

    CollectionBoxResponse depositMoneyToCollectionBox(UUID boxId, DepositMoneyRequest request);

    CollectionBoxResponse emptyCollectionBox(UUID boxId);
}