package br.com.vr.miniautorizador.cartao;

import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class CartaoService {

    static final BigDecimal SALDO_INICIAL = new BigDecimal("500.00");

    private final CartaoRepository repository;
    private final PasswordEncoder passwordEncoder;

    public CartaoService(CartaoRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public void criar(CartaoRequest request) {
        var cartao = new Cartao(request.numeroCartao(), passwordEncoder.encode(request.senha()), SALDO_INICIAL);
        try {
            repository.inserir(cartao);
        } catch (DuplicateKeyException e) {
            throw new CartaoJaExisteException(request);
        }
    }

    public Optional<BigDecimal> consultarSaldo(String numeroCartao) {
        return repository.buscar(numeroCartao).map(Cartao::saldo);
    }
}
