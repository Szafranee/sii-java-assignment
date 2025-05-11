package org.sii.siiassignment.DTO.FundraisingEvent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.sii.siiassignment.Currency;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FundraisingEventResponse {
    private UUID id;
    private String name;
    private Currency accountCurrency;
    private BigDecimal accountBalance;
}