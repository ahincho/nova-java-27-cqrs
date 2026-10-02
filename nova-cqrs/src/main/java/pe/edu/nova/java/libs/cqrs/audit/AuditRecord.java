package pe.edu.nova.java.libs.cqrs.audit;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import pe.edu.nova.java.libs.cqrs.MessageKind;

/**
 * Lo que queda registrado de un mensaje: quién, qué tipo, cuándo, cuánto tardó y cómo terminó (ADR-053).
 *
 * <p>No lleva el contenido del mensaje ni su resultado, que pueden tener datos personales, ni el texto de una
 * excepción, por la misma razón. De un error lleva solo su código.
 *
 * @param kind        si fue un comando o una consulta
 * @param messageType el tipo del mensaje
 * @param actor       quién lo ejecutó, o {@code null} si no hubo un actor
 * @param startedAt   cuándo empezó
 * @param duration    cuánto tardó
 * @param outcome     cómo terminó
 * @param failure     el código del error si no terminó bien, o {@code null}
 */
public record AuditRecord(
        MessageKind kind,
        Class<?> messageType,
        String actor,
        Instant startedAt,
        Duration duration,
        AuditOutcome outcome,
        String failure) {

    /**
     * Valida el registro.
     *
     * @throws NullPointerException si falta un campo obligatorio
     */
    public AuditRecord {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(messageType, "messageType");
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(duration, "duration");
        Objects.requireNonNull(outcome, "outcome");
    }

    /**
     * Quién ejecutó el mensaje.
     *
     * @return el actor, o vacío si no hubo uno
     */
    public Optional<String> actorIfAny() {
        return Optional.ofNullable(actor);
    }

    /**
     * El código del error, si no terminó bien.
     *
     * @return el código, o vacío si terminó bien
     */
    public Optional<String> failureIfAny() {
        return Optional.ofNullable(failure);
    }
}
