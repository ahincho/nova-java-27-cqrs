package pe.edu.nova.java.libs.cqrs;

import java.io.Serial;

/**
 * Dos handlers declaran el mismo tipo de mensaje (ADR-053). En un servicio de Spring Boot impide el arranque, porque
 * el bus no tendría cómo elegir.
 */
public final class DuplicateHandlerException extends IllegalStateException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción.
     *
     * @param messageType el tipo del mensaje
     * @param first       el handler que ya estaba registrado
     * @param second      el que se intentó registrar
     */
    public DuplicateHandlerException(Class<?> messageType, Object first, Object second) {
        super("Two handlers are registered for " + messageType.getName() + ": "
                + first.getClass().getName() + " and " + second.getClass().getName()
                + ". A message type has exactly one handler.");
    }
}
