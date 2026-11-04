package com.rose.carrinho.controller;

import com.rose.carrinho.dto.AdicionarItemRequest;
import com.rose.carrinho.dto.CarrinhoResponse;
import com.rose.carrinho.dto.CriarCarrinhoRequest;
import com.rose.carrinho.service.CarrinhoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/carrinhos")
public class CarrinhoController {

    private final CarrinhoService service;

    public CarrinhoController(CarrinhoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CarrinhoResponse criar(@Valid @RequestBody CriarCarrinhoRequest request) {
        return service.criar(request);
    }

    @GetMapping("/{id}")
    public CarrinhoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping("/{id}/itens")
    public CarrinhoResponse adicionarItem(@PathVariable Long id, @Valid @RequestBody AdicionarItemRequest request) {
        return service.adicionarItem(id, request);
    }

    @DeleteMapping("/{id}/itens/{itemId}")
    public CarrinhoResponse removerItem(@PathVariable Long id, @PathVariable Long itemId) {
        return service.removerItem(id, itemId);
    }

    @PostMapping("/{id}/finalizar")
    public CarrinhoResponse finalizar(@PathVariable Long id) {
        return service.finalizar(id);
    }
}