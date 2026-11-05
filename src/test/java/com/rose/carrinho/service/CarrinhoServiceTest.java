package com.rose.carrinho.service;

import com.rose.carrinho.dto.CarrinhoResponse;
import com.rose.carrinho.model.Carrinho;
import com.rose.carrinho.model.StatusCarrinho;
import com.rose.carrinho.repository.CarrinhoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CarrinhoServiceTest {

    @Mock
    private CarrinhoRepository repository;   // o repository de mentira

    @InjectMocks
    private CarrinhoService service;         // o service de verdade, recebendo o mock

    @Test
    void criarDevolveOCarrinhoAtivoQueJaExiste() {
        // Arrange: "quando procurarem o carrinho ativo da rose, devolva este"
        Carrinho existente = new Carrinho("rose");
        given(repository.findByClienteIdAndStatus("rose", StatusCarrinho.ATIVO))
                .willReturn(Optional.of(existente));

        // Act
        CarrinhoResponse resposta = service.criar("rose");

        // Assert
        assertThat(resposta.clienteId()).isEqualTo("rose");
        verify(repository, never()).save(any());   // não criou um carrinho novo
    }

    @Test
    void criarSalvaUmCarrinhoNovoQuandoNaoExisteAtivo() {
        given(repository.findByClienteIdAndStatus("rose", StatusCarrinho.ATIVO))
                .willReturn(Optional.empty());
        given(repository.save(any(Carrinho.class)))
                .willAnswer(chamada -> chamada.getArgument(0)); // devolve o que recebeu

        CarrinhoResponse resposta = service.criar("rose");

        assertThat(resposta.status()).isEqualTo(StatusCarrinho.ATIVO);
        verify(repository).save(any(Carrinho.class));   // salvou exatamente uma vez
    }

    @Test
    void carrinhoDeOutroClienteRespondeNaoEncontrado() {
        given(repository.findById(1L)).willReturn(Optional.of(new Carrinho("rose")));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.buscar(1L, "ana"));

        assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void carrinhoInexistenteRespondeNaoEncontrado() {
        given(repository.findById(99L)).willReturn(Optional.empty());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.buscar(99L, "rose"));

        assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}