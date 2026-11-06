package com.rose.carrinho.model;

import com.rose.carrinho.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CarrinhoTest {

    @Test
    void carrinhoNovoComecaAtivoEVazio() {
        Carrinho carrinho = new Carrinho("rose");

        assertThat(carrinho.getStatus()).isEqualTo(StatusCarrinho.ATIVO);
        assertThat(carrinho.getItens()).isEmpty();
        assertThat(carrinho.isLembreteEnviado()).isFalse();
    }

    @Test
    void adicionarOMesmoProdutoSomaAQuantidade() {
        Carrinho carrinho = new Carrinho("rose");

        carrinho.adicionarItem("Capinha", 1, new BigDecimal("29.90"));
        carrinho.adicionarItem("Capinha", 2, new BigDecimal("29.90"));

        assertThat(carrinho.getItens()).hasSize(1);
        assertThat(carrinho.getItens().get(0).getQuantidade()).isEqualTo(3);
    }

    @Test
    void totalSomaOsSubtotaisDosItens() {
        Carrinho carrinho = new Carrinho("rose");
        carrinho.adicionarItem("Fone Bluetooth", 1, new BigDecimal("199.90"));
        carrinho.adicionarItem("Capinha", 2, new BigDecimal("29.90"));

        assertThat(carrinho.total()).isEqualByComparingTo("259.70");
    }

    @Test
    void naoFinalizaCarrinhoVazio() {
        Carrinho carrinho = new Carrinho("rose");

        assertThatThrownBy(carrinho::finalizar)
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Carrinho vazio não pode ser finalizado");
    }

    @Test
    void finalizarMudaOStatus() {
        Carrinho carrinho = new Carrinho("rose");
        carrinho.adicionarItem("Fone Bluetooth", 1, new BigDecimal("199.90"));

        carrinho.finalizar();

        assertThat(carrinho.getStatus()).isEqualTo(StatusCarrinho.FINALIZADO);
    }

    @Test
    void naoDeixaAlterarCarrinhoFinalizado() {
        Carrinho carrinho = new Carrinho("rose");
        carrinho.adicionarItem("Fone Bluetooth", 1, new BigDecimal("199.90"));
        carrinho.finalizar();

        assertThatThrownBy(() -> carrinho.adicionarItem("Capinha", 1, new BigDecimal("29.90")))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Carrinho não está ativo");
    }

    @Test
    void marcarAbandonadoMudaStatusERegistraOLembrete() {
        Carrinho carrinho = new Carrinho("rose");

        carrinho.marcarAbandonado();

        assertThat(carrinho.getStatus()).isEqualTo(StatusCarrinho.ABANDONADO);
        assertThat(carrinho.isLembreteEnviado()).isTrue();
    }
}