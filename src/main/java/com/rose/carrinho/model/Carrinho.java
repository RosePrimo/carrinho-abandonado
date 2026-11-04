package com.rose.carrinho.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carrinho",
        indexes = @Index(name = "idx_carrinho_status_atualizado", columnList = "status, atualizado_em"))
public class Carrinho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false)
    private String clienteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusCarrinho status;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Column(name = "lembrete_enviado", nullable = false)
    private boolean lembreteEnviado;

    // UM carrinho tem MUITOS itens
    @OneToMany(mappedBy = "carrinho", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemCarrinho> itens = new ArrayList<>();

    protected Carrinho() {
    }

    public Carrinho(String clienteId) {
        this.clienteId = clienteId;
        this.status = StatusCarrinho.ATIVO;
        this.atualizadoEm = Instant.now();
    }

    public void adicionarItem(String produto, int quantidade, BigDecimal precoUnitario) {
        exigirAtivo();
        itens.stream()
                .filter(i -> i.getProduto().equals(produto))
                .findFirst()
                .ifPresentOrElse(
                        item -> item.somarQuantidade(quantidade),
                        () -> itens.add(new ItemCarrinho(this, produto, quantidade, precoUnitario)));
        tocar();
    }

    public void removerItem(Long itemId) {
        exigirAtivo();
        itens.removeIf(i -> i.getId().equals(itemId));
        tocar();
    }

    public void finalizar() {
        exigirAtivo();
        if (itens.isEmpty()) {
            throw new IllegalStateException("Carrinho vazio não pode ser finalizado");
        }
        this.status = StatusCarrinho.FINALIZADO;
        tocar();
    }

    public void marcarAbandonado() {
        this.status = StatusCarrinho.ABANDONADO;
        this.lembreteEnviado = true;
    }

    public BigDecimal total() {
        return itens.stream().map(ItemCarrinho::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void exigirAtivo() {
        if (status != StatusCarrinho.ATIVO) {
            throw new IllegalStateException("Carrinho não está ativo");
        }
    }

    // toda mexida no carrinho "renova" o relógio do abandono
    private void tocar() {
        this.atualizadoEm = Instant.now();
    }

    public Long getId() { return id; }
    public String getClienteId() { return clienteId; }
    public StatusCarrinho getStatus() { return status; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
    public boolean isLembreteEnviado() { return lembreteEnviado; }
    public List<ItemCarrinho> getItens() { return itens; }
}