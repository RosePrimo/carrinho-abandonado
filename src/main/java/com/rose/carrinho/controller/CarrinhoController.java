package com.rose.carrinho.controller;

import com.rose.carrinho.dto.AdicionarItemRequest;
import com.rose.carrinho.dto.CarrinhoResponse;
import com.rose.carrinho.service.CarrinhoService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/carrinhos")
public class CarrinhoController {

    private final CarrinhoService service;

    public CarrinhoController(CarrinhoService service) {
        this.service = service;
    }

    @Operation(summary = "Criar carrinho", description = "Cria o carrinho ativo do cliente logado, ou devolve o que já existe")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CarrinhoResponse criar(@AuthenticationPrincipal Jwt jwt) {
        return service.criar(jwt.getSubject());
    }

    @Operation(summary = "Buscar carrinho", description = "Devolve o carrinho com os itens e o total")
    @GetMapping("/{id}")
    public CarrinhoResponse buscar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return service.buscar(id, jwt.getSubject());
    }

    @Operation(summary = "Adicionar produto", description = "Adiciona um produto; se já existir, soma a quantidade")
    @PostMapping("/{id}/itens")
    public CarrinhoResponse adicionarItem(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt,
                                          @Valid @RequestBody AdicionarItemRequest request) {
        return service.adicionarItem(id, jwt.getSubject(), request);
    }

    @Operation(summary = "Remover produto")
    @DeleteMapping("/{id}/itens/{itemId}")
    public CarrinhoResponse removerItem(@PathVariable Long id, @PathVariable Long itemId,
                                        @AuthenticationPrincipal Jwt jwt) {
        return service.removerItem(id, jwt.getSubject(), itemId);
    }

    @Operation(summary = "Finalizar compra", description = "Finaliza o carrinho; carrinho vazio não pode ser finalizado")
    @PostMapping("/{id}/finalizar")
    public CarrinhoResponse finalizar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return service.finalizar(id, jwt.getSubject());
    }
}