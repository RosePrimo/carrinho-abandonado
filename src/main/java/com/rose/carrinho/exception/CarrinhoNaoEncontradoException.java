package com.rose.carrinho.exception;

public class CarrinhoNaoEncontradoException extends RuntimeException {

    public CarrinhoNaoEncontradoException(Long id) {
        super("Carrinho " + id + " não encontrado");
    }
}