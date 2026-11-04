package com.rose.carrinho.dto;

import jakarta.validation.constraints.NotBlank;

public record CriarCarrinhoRequest(@NotBlank String clienteId) {
}