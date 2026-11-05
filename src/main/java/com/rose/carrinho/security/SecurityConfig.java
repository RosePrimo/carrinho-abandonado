package com.rose.carrinho.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
public class SecurityConfig {

    /** As regras: quem pode acessar o quê. */
    @Bean
    public SecurityFilterChain filtro(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())                       // API sem sessão/cookie não precisa
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login").permitAll()     // login é público
                        .requestMatchers("/actuator/**").permitAll()    // monitoramento (em produção: rede interna)
                        .anyRequest().authenticated())                  // todo o resto exige token
                .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults())); // valida o "Bearer ..."
        return http.build();
    }

    /** A chave secreta que assina e confere os tokens. */
    @Bean
    public SecretKey chaveJwt(@Value("${jwt.secret}") String segredo) {
        return new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey chave) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey chave) {
        return NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
    }

    /** Senhas guardadas com BCrypt (nunca em texto puro). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Usuários de teste em memória. Num sistema real, viriam do banco. */
    @Bean
    public UserDetailsService usuarios(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("rose").password(encoder.encode("123")).roles("CLIENTE").build(),
                User.withUsername("ana").password(encoder.encode("123")).roles("CLIENTE").build());
    }

    /** Quem confere usuário e senha no login. */
    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService usuarios, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(usuarios);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }
}