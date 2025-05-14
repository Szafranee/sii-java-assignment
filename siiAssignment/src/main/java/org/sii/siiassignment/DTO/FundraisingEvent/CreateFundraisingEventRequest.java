package org.sii.siiassignment.DTO.FundraisingEvent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateFundraisingEventRequest {
    private String name;
    private String accountCurrency;
}