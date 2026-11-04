package com.rose.carrinho.dto;

import com.rose.carrinho.model.ItemCarrinho;

import java.math.BigDecimal;

public record ItemResponse(Long id, String produto, Integer quantidade,
                           BigDecimal precoUnitario, BigDecimal subtotal) {

    public static ItemResponse de(ItemCarrinho item) {
        return new ItemResponse(item.getId(), item.getProduto(), item.getQuantidade(),
                item.getPrecoUnitario(), item.subtotal());
    }
}