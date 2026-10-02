package pe.edu.nova.java.libs.cqrs.audit;

/**
 * Dónde va cada {@link AuditRecord}: el puerto de la auditoría (ADR-053).
 *
 * <p>El starter de Spring Boot trae una línea estructurada en el logger {@code nova.audit}. Una organización que
 * quiera la auditoría en Kafka o en MongoDB declara el suyo.
 *
 * <p>No debería lanzar: el mensaje ya terminó cuando se registra, y su resultado no cambia si el registro falla.
 * Si lanza, {@link AuditBehavior} lo escribe en el log como un error y devuelve el resultado igual.
 */
@FunctionalInterface
public interface AuditSink {

    /**
     * Registra un mensaje.
     *
     * @param record el registro
     */
    void record(AuditRecord record);
}
