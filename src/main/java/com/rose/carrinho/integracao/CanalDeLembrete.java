package com.rose.carrinho.integracao;

/** O contrato: todo canal de lembrete precisa ter um nome e saber enviar. */
public interface CanalDeLembrete {

    String nome();

    void enviar(String clienteId, String mensagem);
}