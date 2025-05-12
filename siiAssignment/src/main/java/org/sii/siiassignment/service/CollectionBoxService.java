package org.sii.siiassignment.service;

import org.sii.siiassignment.DTO.CollectionBox.AddMoneyRequest;
import org.sii.siiassignment.DTO.CollectionBox.AssignBoxToEventRequest;
import org.sii.siiassignment.DTO.CollectionBox.CollectionBoxResponse;
import org.sii.siiassignment.DTO.CollectionBox.CollectionBoxSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface CollectionBoxService {

    CollectionBoxResponse registerCollectionBox();

    List<CollectionBoxSummaryResponse> listAllCollectionBoxes();

    CollectionBoxResponse getCollectionBoxById(UUID id);

    void unregisterCollectionBox(UUID boxId);

    CollectionBoxResponse assignBoxToEvent(UUID boxId, AssignBoxToEventRequest request);

    CollectionBoxResponse addMoneyToBox(UUID boxId, AddMoneyRequest request);

    CollectionBoxResponse emptyCollectionBox(UUID boxId);
}