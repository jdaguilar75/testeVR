package br.com.vr.miniautorizador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Testa os contratos HTTP de ponta a ponta, contra um MongoDB real (mesma versão do docker-compose). */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class MiniAutorizadorApiTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:4.2");

    @Autowired MockMvc mvc;

    @Test
    void fluxoCompletoDoAutorizador() throws Exception {
        var cartao = "1000000000000001";

        criarCartao(cartao, "1234").andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroCartao").value(cartao))
                .andExpect(jsonPath("$.senha").value("1234"));

        saldo(cartao).andExpect(status().isOk()).andExpect(content().string("500.00"));

        // 50 transações de 10,00 esgotam o saldo de 500,00
        for (int i = 0; i < 50; i++) {
            transacao(cartao, "1234", "10.00").andExpect(status().isCreated()).andExpect(content().string("OK"));
        }
        saldo(cartao).andExpect(content().string("0.00"));

        transacao(cartao, "1234", "10.00")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string("SALDO_INSUFICIENTE"));
        transacao(cartao, "senha-errada", "1.00")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string("SENHA_INVALIDA"));
        transacao("9999999999999999", "1234", "1.00")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string("CARTAO_INEXISTENTE"));
    }

    @Test
    void naoCriaCartaoDuplicado() throws Exception {
        criarCartao("2000000000000002", "1234").andExpect(status().isCreated());

        criarCartao("2000000000000002", "1234").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.numeroCartao").value("2000000000000002"))
                .andExpect(jsonPath("$.senha").value("1234"));
    }

    @Test
    void saldoDeCartaoInexistenteRetorna404SemCorpo() throws Exception {
        saldo("0000000000000000").andExpect(status().isNotFound()).andExpect(content().string(""));
    }

    @Test
    void transacoesSimultaneasNuncaDeixamSaldoNegativo() throws Exception {
        var cartao = "3000000000000003";
        criarCartao(cartao, "1234");

        // 100 débitos de 10,00 disputando um saldo de 500,00: exatamente 50 devem passar
        int tentativas = 100;
        var largada = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(20);
        List<Future<String>> resultados = new ArrayList<>();
        for (int i = 0; i < tentativas; i++) {
            Callable<String> tarefa = () -> {
                largada.await();
                return transacao(cartao, "1234", "10.00").andReturn().getResponse().getContentAsString();
            };
            resultados.add(pool.submit(tarefa));
        }
        largada.countDown();

        var respostas = new ArrayList<String>();
        for (var f : resultados) {
            respostas.add(f.get());
        }
        pool.shutdown();

        assertThat(respostas).filteredOn("OK"::equals).hasSize(50);
        assertThat(respostas).filteredOn("SALDO_INSUFICIENTE"::equals).hasSize(50);
        saldo(cartao).andExpect(content().string("0.00"));
    }

    private ResultActions criarCartao(String numero, String senha) throws Exception {
        return mvc.perform(post("/cartoes").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"numeroCartao": "%s", "senha": "%s"}""".formatted(numero, senha)));
    }

    private ResultActions saldo(String numero) throws Exception {
        return mvc.perform(get("/cartoes/" + numero));
    }

    private ResultActions transacao(String numero, String senha, String valor) throws Exception {
        return mvc.perform(post("/transacoes").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"numeroCartao": "%s", "senhaCartao": "%s", "valor": %s}""".formatted(numero, senha, valor)));
    }
}
