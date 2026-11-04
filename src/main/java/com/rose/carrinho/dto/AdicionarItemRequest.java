package com.rose.carrinho.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AdicionarItemRequest(
        @NotBlank String produto,
        @NotNull @Min(1) Integer quantidade,
        @NotNull @DecimalMin("0.01") BigDecimal precoUnitario) {
}