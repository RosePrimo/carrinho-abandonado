package com.rose.carrinho.api;

import com.jayway.jsonpath.JsonPath;
import com.rose.carrinho.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "carrinho.abandono.minutos=600",
        "carrinho.abandono.intervalo-ms=3600000"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@Transactional
class CarrinhoApiTest {  // no fim de cada teste, desfaz tudo no banco (rollback)
    @Autowired
    private MockMvc mvc;

    @Test
    void semTokenRespondeNaoAutorizado() throws Exception {
        mvc.perform(post("/carrinhos"))
                .andExpect(status().isUnauthorized());                       // 401
    }

    @Test
    void loginComSenhaErradaRespondeNaoAutorizado() throws Exception {
        mvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                        .content("""
                                {"usuario":"rose","senha":"errada"}
                                """))
                .andExpect(status().isUnauthorized());                       // 401
    }

    @Test
    void fluxoCompletoDaCompra() throws Exception {
        String token = login("rose");
        Long id = criarCarrinho(token);

        mvc.perform(post("/carrinhos/{id}/itens", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"produto":"Produto Teste","quantidade":2,"precoUnitario":10.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clienteId").value("rose"))
                .andExpect(jsonPath("$.itens[*].produto", hasItem("Produto Teste")));

        mvc.perform(post("/carrinhos/{id}/finalizar", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADO"));
    }

    @Test
    void anaNaoEnxergaOCarrinhoDaRose() throws Exception {
        Long idDaRose = criarCarrinho(login("rose"));
        String tokenDaAna = login("ana");

        mvc.perform(get("/carrinhos/{id}", idDaRose)
                        .header("Authorization", "Bearer " + tokenDaAna))
                .andExpect(status().isNotFound());                           // 404
    }

    // ---------- ajudantes ----------

    private String login(String usuario) throws Exception {
        String resposta = mvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"usuario\":\"" + usuario + "\",\"senha\":\"123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.token");
    }

    private Long criarCarrinho(String token) throws Exception {
        String resposta = mvc.perform(post("/carrinhos").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())                             // 201
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(resposta, "$.id");
        return id.longValue();
    }
}