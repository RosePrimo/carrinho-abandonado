package com.rose.carrinho.job;

import com.rose.carrinho.model.Carrinho;
import com.rose.carrinho.repository.CarrinhoRepository;
import com.rose.carrinho.service.LembreteService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class AbandonoCarrinhoJob {

    private static final Logger log = LoggerFactory.getLogger(AbandonoCarrinhoJob.class);

    private final CarrinhoRepository repository;
    private final LembreteService lembrete;
    private final long minutosParaAbandono;
    private final Counter carrinhosAbandonados;

    public AbandonoCarrinhoJob(CarrinhoRepository repository, LembreteService lembrete, MeterRegistry metricas,
                               @Value("${carrinho.abandono.minutos:30}") long minutosParaAbandono) {
        this.repository = repository;
        this.lembrete = lembrete;
        this.minutosParaAbandono = minutosParaAbandono;
        this.carrinhosAbandonados = Counter.builder("carrinho.abandonados")
                .description("Quantidade de carrinhos marcados como abandonados")
                .register(metricas);
    }

    @Scheduled(fixedDelayString = "${carrinho.abandono.intervalo-ms:60000}")
    @Transactional
    public void verificar() {
        Instant limite = Instant.now().minus(Duration.ofMinutes(minutosParaAbandono));
        List<Carrinho> parados = repository.travarParadosParaAbandono(limite);

        for (Carrinho carrinho : parados) {
            carrinho.marcarAbandonado();
            if (!carrinho.getItens().isEmpty()) {
                lembrete.enviar(carrinho);
            }
        }
        if (!parados.isEmpty()) {
            carrinhosAbandonados.increment(parados.size());
            log.info("{} carrinho(s) marcado(s) como abandonado(s)", parados.size());
        }
    }
}