package com.rose.carrinho.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "lembrete")
public class Lembrete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "carrinho_id", nullable = false)
    private Long carrinhoId;

    @Column(name = "cliente_id", nullable = false)
    private String clienteId;

    // qual instância enviou (no Docker, é o id do container)
    @Column(nullable = false)
    private String instancia;

    @Column(name = "enviado_em", nullable = false)
    private Instant enviadoEm;

    protected Lembrete() {
    }

    public Lembrete(Long carrinhoId, String clienteId, String instancia) {
        this.carrinhoId = carrinhoId;
        this.clienteId = clienteId;
        this.instancia = instancia;
        this.enviadoEm = Instant.now();
    }
}