package com.rose.carrinho.integracao;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedRetryMetrics;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ResilienceConfig {

    /** A "fábrica" de circuit breakers: cada canal pede o seu, pelo nome. */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(MeterRegistry metricas) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)                           // olha as últimas 10 chamadas
                .minimumNumberOfCalls(5)                         // só decide depois de pelo menos 5
                .failureRateThreshold(50)                        // 50% de falha → ABRE
                .waitDurationInOpenState(Duration.ofSeconds(30)) // fica aberto 30 s
                .permittedNumberOfCallsInHalfOpenState(2)        // depois testa com 2 chamadas
                .build();
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry).bindTo(metricas);
        return registry;
    }

    /** A "fábrica" de retries: cada canal pede o seu, pelo nome. */
    @Bean
    public RetryRegistry retryRegistry(MeterRegistry metricas) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)                                  // até 3 tentativas no total
                .intervalFunction(IntervalFunction.ofExponentialBackoff(Duration.ofMillis(200), 2))
                // espera 200 ms, depois 400 ms
                .retryExceptions(RuntimeException.class)         // tenta de novo em qualquer erro do canal...
                .ignoreExceptions(CallNotPermittedException.class) // ...menos com o disjuntor aberto
                .build();
        RetryRegistry registry = RetryRegistry.of(config);
        TaggedRetryMetrics.ofRetryRegistry(registry).bindTo(metricas);
        return registry;
    }
}