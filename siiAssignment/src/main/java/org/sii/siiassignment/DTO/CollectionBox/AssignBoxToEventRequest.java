package org.sii.siiassignment.DTO.CollectionBox;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignBoxToEventRequest {
    private UUID fundraisingEventId;
}
