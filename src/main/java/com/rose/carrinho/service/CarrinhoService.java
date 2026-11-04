package com.rose.carrinho.service;

import com.rose.carrinho.dto.AdicionarItemRequest;
import com.rose.carrinho.dto.CarrinhoResponse;
import com.rose.carrinho.dto.CriarCarrinhoRequest;
import com.rose.carrinho.model.Carrinho;
import com.rose.carrinho.model.StatusCarrinho;
import com.rose.carrinho.repository.CarrinhoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CarrinhoService {

    private final CarrinhoRepository repository;

    public CarrinhoService(CarrinhoRepository repository) {
        this.repository = repository;
    }

    /** Regra: cada cliente tem no máximo UM carrinho ativo. Se já tem, devolve o mesmo. */
    @Transactional
    public CarrinhoResponse criar(CriarCarrinhoRequest request) {
        Carrinho carrinho = repository.findByClienteIdAndStatus(request.clienteId(), StatusCarrinho.ATIVO)
                .orElseGet(() -> repository.save(new Carrinho(request.clienteId())));
        return CarrinhoResponse.de(carrinho);
    }

    @Transactional(readOnly = true)
    public CarrinhoResponse buscar(Long id) {
        return CarrinhoResponse.de(buscarCarrinho(id));
    }

    @Transactional
    public CarrinhoResponse adicionarItem(Long id, AdicionarItemRequest request) {
        Carrinho carrinho = buscarCarrinho(id);
        carrinho.adicionarItem(request.produto(), request.quantidade(), request.precoUnitario());
        // saveAndFlush: grava agora, para o item novo já voltar com o id
        return CarrinhoResponse.de(repository.saveAndFlush(carrinho));
    }

    @Transactional
    public CarrinhoResponse removerItem(Long id, Long itemId) {
        Carrinho carrinho = buscarCarrinho(id);
        carrinho.removerItem(itemId);
        return CarrinhoResponse.de(carrinho);
    }

    @Transactional
    public CarrinhoResponse finalizar(Long id) {
        Carrinho carrinho = buscarCarrinho(id);
        carrinho.finalizar();
        return CarrinhoResponse.de(carrinho);
    }

    private Carrinho buscarCarrinho(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carrinho não encontrado"));
    }
}