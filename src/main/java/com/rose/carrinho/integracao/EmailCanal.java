package com.rose.carrinho.integracao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Simula o envio de e-mail (como um SendGrid ou Amazon SES). */
@Component
public class EmailCanal implements CanalDeLembrete {

    private static final Logger log = LoggerFactory.getLogger(EmailCanal.class);

    @Override
    public String nome() {
        return "email";
    }

    @Override
    public void enviar(String clienteId, String mensagem) {
        log.info("E-MAIL enviado para {}@email.com: {}", clienteId, mensagem);
    }
}