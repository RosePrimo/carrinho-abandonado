package com.rose.carrinho.service;

import com.rose.carrinho.model.Carrinho;
import com.rose.carrinho.model.Lembrete;
import com.rose.carrinho.repository.LembreteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LembreteService {

    private static final Logger log = LoggerFactory.getLogger(LembreteService.class);
    private static final String INSTANCIA = System.getenv().getOrDefault("HOSTNAME", "local");

    private final LembreteRepository repository;

    public LembreteService(LembreteRepository repository) {
        this.repository = repository;
    }

    public void enviar(Carrinho carrinho) {
        try {
            Thread.sleep(1000); // simula a chamada ao serviço de push (rede lenta)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        repository.save(new Lembrete(carrinho.getId(), carrinho.getClienteId(), INSTANCIA));
        log.info("LEMBRETE para {} (carrinho {})", carrinho.getClienteId(), carrinho.getId());
    }
}