package com.rose.carrinho.controller;

import com.rose.carrinho.integracao.PushClient;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/push")
public class AdminController {

    private final PushClient pushClient;

    public AdminController(PushClient pushClient) {
        this.pushClient = pushClient;
    }

    @Operation(summary = "Simular falhas no push", description = "0.0 = nunca falha; 0.3 = 30% de falha; 1.0 = fora do ar")
    @PostMapping("/taxa-falha")
    public Map<String, Double> mudarTaxa(@RequestParam double valor) {
        pushClient.setTaxaFalha(valor);
        return Map.of("taxaFalha", valor);
    }
}