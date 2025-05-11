package org.sii.siiassignment.DTO.CollectionBox;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.sii.siiassignment.Currency;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddMoneyRequest {
    private Currency currency;
    private BigDecimal amount;
}