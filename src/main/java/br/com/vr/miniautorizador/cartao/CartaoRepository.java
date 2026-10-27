package br.com.vr.miniautorizador.cartao;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class CartaoRepository {

    private final MongoTemplate mongo;

    public CartaoRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    /** @throws org.springframework.dao.DuplicateKeyException se o número já existir */
    public void inserir(Cartao cartao) {
        mongo.insert(cartao);
    }

    public Optional<Cartao> buscar(String numero) {
        return Optional.ofNullable(mongo.findById(numero, Cartao.class));
    }

    /**
     * Debita o valor em uma única operação atômica: a checagem de saldo e o decremento
     * acontecem juntos no banco, então transações simultâneas (inclusive de instâncias
     * diferentes da aplicação) nunca deixam o saldo negativo.
     *
     * @return o cartão já atualizado, ou vazio se o saldo for insuficiente
     */
    public Optional<Cartao> debitar(String numero, BigDecimal valor) {
        var filtro = query(where("_id").is(numero).and("saldo").gte(valor));
        var decremento = new Update().inc("saldo", valor.negate());
        return Optional.ofNullable(
                mongo.findAndModify(filtro, decremento, FindAndModifyOptions.options().returnNew(true), Cartao.class));
    }
}
