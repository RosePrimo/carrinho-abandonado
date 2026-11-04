package com.rose.carrinho.dto;

import com.rose.carrinho.model.Carrinho;
import com.rose.carrinho.model.StatusCarrinho;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CarrinhoResponse(Long id, String clienteId, StatusCarrinho status,
                               Instant atualizadoEm, List<ItemResponse> itens, BigDecimal total) {

    public static CarrinhoResponse de(Carrinho c) {
        return new CarrinhoResponse(c.getId(), c.getClienteId(), c.getStatus(), c.getAtualizadoEm(),
                c.getItens().stream().map(ItemResponse::de).toList(), c.total());
    }
}