package br.com.vr.miniautorizador.cartao;

import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** O número do cartão é o {@code _id}, o que garante unicidade no banco. */
@Document("cartoes")
public record Cartao(@Id String numero, String senhaHash, BigDecimal saldo) {
}
