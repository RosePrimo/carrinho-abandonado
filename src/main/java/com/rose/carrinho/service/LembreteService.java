package com.rose.carrinho.service;

import com.rose.carrinho.integracao.PushClient;
import com.rose.carrinho.integracao.PushIndisponivelException;
import com.rose.carrinho.model.Carrinho;
import com.rose.carrinho.model.Lembrete;
import com.rose.carrinho.repository.LembreteRepository;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LembreteService {

    private static final Logger log = LoggerFactory.getLogger(LembreteService.class);
    private static final String INSTANCIA = System.getenv().getOrDefault("HOSTNAME", "local");

    private final LembreteRepository repository;
    private final PushClient pushClient;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final Counter lembretesEnviados;
    private final Counter lembretesFalhos;

    public LembreteService(LembreteRepository repository, PushClient pushClient, Retry retry,
                           CircuitBreaker circuitBreaker, MeterRegistry metricas) {
        this.repository = repository;
        this.pushClient = pushClient;
        this.retry = retry;
        this.circuitBreaker = circuitBreaker;
        this.lembretesEnviados = Counter.builder("carrinho.lembretes.enviados")
                .description("Lembretes de carrinho abandonado enviados").register(metricas);
        this.lembretesFalhos = Counter.builder("carrinho.lembretes.falhos")
                .description("Lembretes que não puderam ser enviados").register(metricas);
    }

    /** Devolve true se o lembrete foi entregue. */
    public boolean enviar(Carrinho carrinho) {
        String mensagem = "Você esqueceu " + carrinho.getItens().size() + " item(ns) no carrinho (total R$ "
                + carrinho.total() + "). Volte e finalize sua compra!";

        // a chamada ao serviço externo, "embrulhada": Retry por fora, Circuit Breaker por dentro
        Runnable chamada = () -> pushClient.enviar(carrinho.getClienteId(), mensagem);
        Runnable protegida = Retry.decorateRunnable(retry,
                CircuitBreaker.decorateRunnable(circuitBreaker, chamada));

        try {
            protegida.run();
            repository.save(new Lembrete(carrinho.getId(), carrinho.getClienteId(), INSTANCIA));
            lembretesEnviados.increment();
            return true;
        } catch (CallNotPermittedException e) {
            log.warn("Circuit breaker ABERTO: lembrete de {} não enviado (o push nem foi chamado)",
                    carrinho.getClienteId());
        } catch (PushIndisponivelException e) {
            log.warn("Push falhou nas 3 tentativas: lembrete de {} não enviado", carrinho.getClienteId());
        }
        lembretesFalhos.increment();
        return false;
    }
}