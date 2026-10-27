package br.com.vr.miniautorizador.transacao;

import static br.com.vr.miniautorizador.transacao.MotivoRecusa.*;

import br.com.vr.miniautorizador.cartao.CartaoRepository;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class TransacaoService {

    private final CartaoRepository repository;
    private final PasswordEncoder passwordEncoder;

    public TransacaoService(CartaoRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Aplica as regras de autorização na ordem: cartão existe, senha correta, saldo disponível.
     * Qualquer falha lança {@link TransacaoRecusadaException} com o motivo.
     */
    public void autorizar(TransacaoRequest transacao) {
        var cartao = repository.buscar(transacao.numeroCartao())
                .orElseThrow(recusa(CARTAO_INEXISTENTE));

        Optional.of(cartao)
                .filter(c -> passwordEncoder.matches(transacao.senhaCartao(), c.senhaHash()))
                .orElseThrow(recusa(SENHA_INVALIDA));

        // A checagem de saldo é feita pelo próprio débito atômico (ver CartaoRepository#debitar),
        // evitando a corrida de "ler o saldo e depois gravar".
        repository.debitar(transacao.numeroCartao(), transacao.valor())
                .orElseThrow(recusa(SALDO_INSUFICIENTE));
    }

    private static Supplier<TransacaoRecusadaException> recusa(MotivoRecusa motivo) {
        return () -> new TransacaoRecusadaException(motivo);
    }
}
