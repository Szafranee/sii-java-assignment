package org.sii.siiassignment.DTO.FundraisingEvent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.sii.siiassignment.Currency;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateFundraisingEventRequest {
    private String name;
    private Currency accountCurrency; // Or String if you prefer to validate in service
}