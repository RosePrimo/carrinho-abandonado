package com.rose.carrinho.service;

import com.rose.carrinho.integracao.CanalDeLembrete;
import com.rose.carrinho.model.Carrinho;
import com.rose.carrinho.model.Lembrete;
import com.rose.carrinho.repository.LembreteRepository;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LembreteService {

    private static final Logger log = LoggerFactory.getLogger(LembreteService.class);
    private static final String INSTANCIA = System.getenv().getOrDefault("HOSTNAME", "local");

    private final LembreteRepository repository;
    private final List<CanalDeLembrete> canais;          // TODOS os canais que existirem
    private final CircuitBreakerRegistry circuitBreakers;
    private final RetryRegistry retries;
    private final Counter lembretesEnviados;
    private final Counter lembretesFalhos;

    public LembreteService(LembreteRepository repository, List<CanalDeLembrete> canais,
                           CircuitBreakerRegistry circuitBreakers, RetryRegistry retries,
                           MeterRegistry metricas) {
        this.repository = repository;
        this.canais = canais;
        this.circuitBreakers = circuitBreakers;
        this.retries = retries;
        this.lembretesEnviados = Counter.builder("carrinho.lembretes.enviados")
                .description("Lembretes entregues por pelo menos um canal").register(metricas);
        this.lembretesFalhos = Counter.builder("carrinho.lembretes.falhos")
                .description("Lembretes que não chegaram por nenhum canal").register(metricas);

        // cada canal ganha o seu próprio circuit breaker e retry, com log
        for (CanalDeLembrete canal : canais) {
            circuitBreakers.circuitBreaker(canal.nome()).getEventPublisher().onStateTransition(e ->
                    log.warn("CIRCUIT BREAKER do {} mudou: {}", canal.nome(), e.getStateTransition()));
            retries.retry(canal.nome()).getEventPublisher().onRetry(e ->
                    log.info("{} falhou (tentativa {}). Tentando de novo em {} ms",
                            canal.nome(), e.getNumberOfRetryAttempts(), e.getWaitInterval().toMillis()));
        }
        log.info("Canais de lembrete disponíveis: {}", canais.stream().map(CanalDeLembrete::nome).toList());
    }

    /** Tenta todos os canais. Devolve true se pelo menos um entregou. */
    public boolean enviar(Carrinho carrinho) {
        String mensagem = "Você esqueceu " + carrinho.getItens().size() + " item(ns) no carrinho (total R$ "
                + carrinho.total() + "). Volte e finalize sua compra!";

        boolean entregou = false;
        for (CanalDeLembrete canal : canais) {
            if (enviarPor(canal, carrinho.getClienteId(), mensagem)) {
                entregou = true;
            }
        }

        if (entregou) {
            repository.save(new Lembrete(carrinho.getId(), carrinho.getClienteId(), INSTANCIA));
            lembretesEnviados.increment();
        } else {
            lembretesFalhos.increment();
        }
        return entregou;
    }

    private boolean enviarPor(CanalDeLembrete canal, String clienteId, String mensagem) {
        Runnable chamada = () -> canal.enviar(clienteId, mensagem);   // não sabe QUAL canal é
        Runnable protegida = Retry.decorateRunnable(retries.retry(canal.nome()),
                CircuitBreaker.decorateRunnable(circuitBreakers.circuitBreaker(canal.nome()), chamada));
        try {
            protegida.run();
            return true;
        } catch (CallNotPermittedException e) {
            log.warn("Circuit breaker do {} ABERTO: {} não recebeu por esse canal", canal.nome(), clienteId);
        } catch (RuntimeException e) {
            log.warn("{} falhou de vez para {}: {}", canal.nome(), clienteId, e.getMessage());
        }
        return false;
    }
}