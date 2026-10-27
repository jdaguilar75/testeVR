package br.com.vr.miniautorizador.transacao;

public class TransacaoRecusadaException extends RuntimeException {

    private final MotivoRecusa motivo;

    public TransacaoRecusadaException(MotivoRecusa motivo) {
        super(motivo.name());
        this.motivo = motivo;
    }

    public MotivoRecusa getMotivo() {
        return motivo;
    }
}
