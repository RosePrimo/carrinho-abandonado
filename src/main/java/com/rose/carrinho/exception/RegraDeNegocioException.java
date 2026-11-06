package com.rose.carrinho.exception;

/** Uma regra do negócio foi violada (ex.: finalizar carrinho vazio). */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}