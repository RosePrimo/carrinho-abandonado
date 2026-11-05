package com.rose.carrinho.service;

import com.rose.carrinho.dto.AdicionarItemRequest;
import com.rose.carrinho.dto.CarrinhoResponse;
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

    @Transactional
    public CarrinhoResponse criar(String clienteId) {
        Carrinho carrinho = repository.findByClienteIdAndStatus(clienteId, StatusCarrinho.ATIVO)
                .orElseGet(() -> repository.save(new Carrinho(clienteId)));
        return CarrinhoResponse.de(carrinho);
    }

    @Transactional(readOnly = true)
    public CarrinhoResponse buscar(Long id, String clienteId) {
        return CarrinhoResponse.de(buscarDoCliente(id, clienteId));
    }

    @Transactional
    public CarrinhoResponse adicionarItem(Long id, String clienteId, AdicionarItemRequest request) {
        Carrinho carrinho = buscarDoCliente(id, clienteId);
        carrinho.adicionarItem(request.produto(), request.quantidade(), request.precoUnitario());
        return CarrinhoResponse.de(repository.saveAndFlush(carrinho));
    }

    @Transactional
    public CarrinhoResponse removerItem(Long id, String clienteId, Long itemId) {
        Carrinho carrinho = buscarDoCliente(id, clienteId);
        carrinho.removerItem(itemId);
        return CarrinhoResponse.de(carrinho);
    }

    @Transactional
    public CarrinhoResponse finalizar(Long id, String clienteId) {
        Carrinho carrinho = buscarDoCliente(id, clienteId);
        carrinho.finalizar();
        return CarrinhoResponse.de(carrinho);
    }

    /** Carrinho de outra pessoa responde 404, como se não existisse: não revela que ele existe. */
    private Carrinho buscarDoCliente(Long id, String clienteId) {
        return repository.findById(id)
                .filter(c -> c.getClienteId().equals(clienteId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carrinho não encontrado"));
    }
}