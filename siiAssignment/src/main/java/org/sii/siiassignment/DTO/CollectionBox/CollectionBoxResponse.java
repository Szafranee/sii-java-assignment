package org.sii.siiassignment.DTO.CollectionBox;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.sii.siiassignment.Currency;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollectionBoxResponse {
    private UUID id;
    private UUID fundraisingEventId;
    private Map<Currency, BigDecimal> amounts;
    private boolean isEmpty;
    private boolean isAssigned;
}