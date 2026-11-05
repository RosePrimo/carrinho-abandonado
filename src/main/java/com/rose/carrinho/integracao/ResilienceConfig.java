package com.rose.carrinho.integracao;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedRetryMetrics;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ResilienceConfig {

    private static final Logger log = LoggerFactory.getLogger(ResilienceConfig.class);

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(MeterRegistry metricas) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)                          // olha as últimas 10 chamadas
                .minimumNumberOfCalls(5)                        // só decide depois de pelo menos 5
                .failureRateThreshold(50)                       // 50% de falha → ABRE
                .waitDurationInOpenState(Duration.ofSeconds(30))// fica aberto 30 s
                .permittedNumberOfCallsInHalfOpenState(2)       // depois testa com 2 chamadas
                .build();
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry).bindTo(metricas);
        return registry;
    }

    @Bean
    public CircuitBreaker pushCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreaker circuitBreaker = registry.circuitBreaker("push");
        circuitBreaker.getEventPublisher().onStateTransition(evento ->
                log.warn("CIRCUIT BREAKER do push mudou: {}", evento.getStateTransition()));
        return circuitBreaker;
    }

    @Bean
    public RetryRegistry retryRegistry(MeterRegistry metricas) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)                                  // até 3 tentativas no total
                .intervalFunction(IntervalFunction.ofExponentialBackoff(Duration.ofMillis(200), 2))
                // espera 200 ms, depois 400 ms
                .retryExceptions(PushIndisponivelException.class)// só tenta de novo nesse erro
                .build();
        RetryRegistry registry = RetryRegistry.of(config);
        TaggedRetryMetrics.ofRetryRegistry(registry).bindTo(metricas);
        return registry;
    }

    @Bean
    public Retry pushRetry(RetryRegistry registry) {
        Retry retry = registry.retry("push");
        retry.getEventPublisher().onRetry(evento ->
                log.info("Push falhou (tentativa {}). Tentando de novo em {} ms",
                        evento.getNumberOfRetryAttempts(), evento.getWaitInterval().toMillis()));
        return retry;
    }
}