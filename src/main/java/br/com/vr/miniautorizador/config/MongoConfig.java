package br.com.vr.miniautorizador.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions.BigDecimalRepresentation;

@Configuration
class MongoConfig {

    /**
     * Por padrão o BigDecimal é gravado como String, e o operador $inc (usado no débito atômico do saldo)
     * só funciona com tipos numéricos. Decimal128 mantém a precisão monetária e permite o $inc.
     */
    @Bean
    MongoCustomConversions mongoCustomConversions() {
        return MongoCustomConversions.create(config -> config.bigDecimal(BigDecimalRepresentation.DECIMAL128));
    }
}
