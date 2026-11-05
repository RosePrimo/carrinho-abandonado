package com.rose.carrinho.controller;

import com.rose.carrinho.dto.LoginRequest;
import com.rose.carrinho.dto.TokenResponse;
import com.rose.carrinho.security.TokenService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public AuthController(AuthenticationManager authenticationManager, TokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication autenticado = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.usuario(), request.senha()));
        return new TokenResponse(tokenService.gerar(autenticado), "Bearer", tokenService.getExpiracaoSegundos());
    }
}