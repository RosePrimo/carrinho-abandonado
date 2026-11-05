package com.rose.carrinho.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final long expiracaoSegundos;

    public TokenService(JwtEncoder encoder, @Value("${jwt.expiracao-segundos}") long expiracaoSegundos) {
        this.encoder = encoder;
        this.expiracaoSegundos = expiracaoSegundos;
    }

    public String gerar(Authentication autenticacao) {
        Instant agora = Instant.now();
        JwtClaimsSet conteudo = JwtClaimsSet.builder()
                .issuer("carrinho-api")                          // quem emitiu
                .subject(autenticacao.getName())                 // quem é: "rose"
                .issuedAt(agora)
                .expiresAt(agora.plusSeconds(expiracaoSegundos)) // vale 1 hora
                .claim("roles", autenticacao.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority).toList())
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(cabecalho, conteudo)).getTokenValue();
    }

    public long getExpiracaoSegundos() {
        return expiracaoSegundos;
    }
}