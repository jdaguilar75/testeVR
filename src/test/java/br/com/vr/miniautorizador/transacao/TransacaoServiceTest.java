package br.com.vr.miniautorizador.transacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.vr.miniautorizador.cartao.Cartao;
import br.com.vr.miniautorizador.cartao.CartaoRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class TransacaoServiceTest {

    private static final String NUMERO = "6549873025634501";
    private static final BigDecimal VALOR = new BigDecimal("10.00");
    private static final Cartao CARTAO = new Cartao(NUMERO, "hash", new BigDecimal("500.00"));

    @Mock CartaoRepository repository;
    @Mock PasswordEncoder passwordEncoder;

    TransacaoService service;

    @BeforeEach
    void setUp() {
        service = new TransacaoService(repository, passwordEncoder);
    }

    @Test
    void autorizaQuandoTodasAsRegrasSaoAtendidas() {
        when(repository.buscar(NUMERO)).thenReturn(Optional.of(CARTAO));
        when(passwordEncoder.matches("1234", "hash")).thenReturn(true);
        when(repository.debitar(NUMERO, VALOR)).thenReturn(Optional.of(CARTAO));

        assertThatCode(() -> service.autorizar(new TransacaoRequest(NUMERO, "1234", VALOR)))
                .doesNotThrowAnyException();
    }

    @Test
    void recusaCartaoInexistente() {
        when(repository.buscar(NUMERO)).thenReturn(Optional.empty());

        assertRecusa(MotivoRecusa.CARTAO_INEXISTENTE);
        verify(repository, never()).debitar(any(), any());
    }

    @Test
    void recusaSenhaInvalidaSemDebitar() {
        when(repository.buscar(NUMERO)).thenReturn(Optional.of(CARTAO));
        when(passwordEncoder.matches("1234", "hash")).thenReturn(false);

        assertRecusa(MotivoRecusa.SENHA_INVALIDA);
        verify(repository, never()).debitar(any(), any());
    }

    @Test
    void recusaSaldoInsuficiente() {
        when(repository.buscar(NUMERO)).thenReturn(Optional.of(CARTAO));
        when(passwordEncoder.matches("1234", "hash")).thenReturn(true);
        when(repository.debitar(NUMERO, VALOR)).thenReturn(Optional.empty());

        assertRecusa(MotivoRecusa.SALDO_INSUFICIENTE);
    }

    private void assertRecusa(MotivoRecusa esperado) {
        assertThatThrownBy(() -> service.autorizar(new TransacaoRequest(NUMERO, "1234", VALOR)))
                .isInstanceOfSatisfying(TransacaoRecusadaException.class,
                        e -> assertThat(e.getMotivo()).isEqualTo(esperado));
    }
}
