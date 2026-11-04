package com.rose.carrinho.service;

import com.rose.carrinho.model.Carrinho;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LembreteService {

    private static final Logger log = LoggerFactory.getLogger(LembreteService.class);

    public void enviar(Carrinho carrinho) {
        log.info("LEMBRETE para {}: você esqueceu {} item(ns) no carrinho {} (total R$ {}). Volte e finalize sua compra!",
                carrinho.getClienteId(), carrinho.getItens().size(), carrinho.getId(), carrinho.total());
    }
}