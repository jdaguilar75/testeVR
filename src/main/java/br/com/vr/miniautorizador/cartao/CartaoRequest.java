package br.com.vr.miniautorizador.cartao;

import jakarta.validation.constraints.NotBlank;

/** Corpo da criação de cartão; o contrato devolve o mesmo formato na resposta. */
public record CartaoRequest(@NotBlank String numeroCartao, @NotBlank String senha) {
}
