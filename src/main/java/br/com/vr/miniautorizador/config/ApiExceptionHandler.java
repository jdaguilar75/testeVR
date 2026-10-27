package br.com.vr.miniautorizador.config;

import br.com.vr.miniautorizador.cartao.CartaoJaExisteException;
import br.com.vr.miniautorizador.cartao.CartaoRequest;
import br.com.vr.miniautorizador.transacao.TransacaoRecusadaException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduz as exceções de negócio para as respostas definidas no contrato. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(CartaoJaExisteException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    CartaoRequest cartaoJaExiste(CartaoJaExisteException e) {
        return e.getCartao();
    }

    @ExceptionHandler(TransacaoRecusadaException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    String transacaoRecusada(TransacaoRecusadaException e) {
        return e.getMotivo().name();
    }
}
