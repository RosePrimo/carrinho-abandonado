package com.rose.carrinho.integracao;

public class PushIndisponivelException extends RuntimeException {
    public PushIndisponivelException(String mensagem) {
        super(mensagem);
    }
}