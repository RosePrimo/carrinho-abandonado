package com.rose.carrinho.integracao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/** Simula um serviço externo de push (como o Firebase), que às vezes falha. */
@Component
public class PushClient {

    private static final Logger log = LoggerFactory.getLogger(PushClient.class);

    private volatile double taxaFalha = 0.0;   // 0.0 = nunca falha; 1.0 = sempre falha

    public void enviar(String clienteId, String mensagem) {
        try {
            Thread.sleep(200);   // a chamada pela rede
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (ThreadLocalRandom.current().nextDouble() < taxaFalha) {
            throw new PushIndisponivelException("Serviço de push indisponível");
        }
        log.info("PUSH entregue para {}: {}", clienteId, mensagem);
    }

    public double getTaxaFalha() { return taxaFalha; }
    public void setTaxaFalha(double taxaFalha) { this.taxaFalha = taxaFalha; }
}