package org.sii.siiassignment.DTO.FundraisingEvent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.sii.siiassignment.Currency;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancialReportEntry {
    private String fundraisingEventName;
    private BigDecimal amount;
    private Currency currency;
}