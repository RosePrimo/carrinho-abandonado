package com.rose.carrinho.dto;

public record TokenResponse(String token, String tipo, long expiraEmSegundos) {
}