package org.sii.siiassignment.DTO.FundraisingEvent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancialReportEntry {
    private String fundraisingEventName;
    private BigDecimal amount;
    private String currency;
}