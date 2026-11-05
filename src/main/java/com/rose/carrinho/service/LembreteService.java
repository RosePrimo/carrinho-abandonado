package com.rose.carrinho.service;

import com.rose.carrinho.model.Carrinho;
import com.rose.carrinho.model.Lembrete;
import com.rose.carrinho.repository.LembreteRepository;
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
    private final Counter lembretesEnviados;

    public LembreteService(LembreteRepository repository, MeterRegistry metricas) {
        this.repository = repository;
        this.lembretesEnviados = Counter.builder("carrinho.lembretes.enviados")
                .description("Quantidade de lembretes de carrinho abandonado enviados")
                .register(metricas);
    }

    public void enviar(Carrinho carrinho) {
        try {
            Thread.sleep(1000); // simula a chamada ao serviço de push (rede lenta)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        repository.save(new Lembrete(carrinho.getId(), carrinho.getClienteId(), INSTANCIA));
        lembretesEnviados.increment();
        log.info("LEMBRETE para {} (carrinho {})", carrinho.getClienteId(), carrinho.getId());
    }
}