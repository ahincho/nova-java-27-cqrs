package pe.edu.nova.java.starters.cqrs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.edu.nova.java.libs.cqrs.audit.AuditRecord;
import pe.edu.nova.java.libs.cqrs.audit.AuditSink;

/**
 * El destino de auditoría por defecto: una línea en el logger {@code nova.audit}, que llega a la pila de
 * observabilidad como cualquier log del servicio (ADR-053).
 *
 * <p>Cada campo va también como par clave-valor, así que un log estructurado de Spring Boot lo trae como campos
 * propios. No lleva el contenido del mensaje: {@link AuditRecord} no lo tiene.
 */
public final class LoggingAuditSink implements AuditSink {

    /** El nombre del logger, para dirigirlo a otro appender o subirle el nivel. */
    public static final String LOGGER = "nova.audit";

    private static final Logger LOG = LoggerFactory.getLogger(LOGGER);

    /** Crea el destino. */
    public LoggingAuditSink() {}

    @Override
    public void record(AuditRecord record) {
        String actor = record.actorIfAny().orElse("-");
        String failure = record.failureIfAny().orElse("-");
        LOG.atInfo()
                .addKeyValue("audit.kind", record.kind())
                .addKeyValue("audit.message", record.messageType().getName())
                .addKeyValue("audit.actor", actor)
                .addKeyValue("audit.startedAt", record.startedAt())
                .addKeyValue("audit.durationMs", record.duration().toMillis())
                .addKeyValue("audit.outcome", record.outcome())
                .addKeyValue("audit.failure", failure)
                .log(
                        "{} {} by {}: {} {} in {} ms",
                        record.kind(),
                        record.messageType().getSimpleName(),
                        actor,
                        record.outcome(),
                        failure,
                        record.duration().toMillis());
    }
}
