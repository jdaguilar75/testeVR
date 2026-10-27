package br.com.vr.miniautorizador.cartao;

public class CartaoJaExisteException extends RuntimeException {

    private final CartaoRequest cartao;

    public CartaoJaExisteException(CartaoRequest cartao) {
        this.cartao = cartao;
    }

    public CartaoRequest getCartao() {
        return cartao;
    }
}
