package pe.edu.nova.java.libs.cqrs;

import java.io.Serial;

/**
 * Un mensaje llegó al bus y ningún handler lo ejecuta (ADR-053).
 *
 * <p>Es un defecto del servicio, no del cliente: en un servicio con el estándar de API de Nova se responde como un
 * error de plataforma, un 500 sin el detalle, que va al log.
 */
public final class HandlerNotFoundException extends IllegalStateException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** El tipo del mensaje que no tiene handler. */
    private final transient Class<?> messageType;

    /**
     * Crea la excepción.
     *
     * @param messageType el tipo del mensaje que no tiene handler
     */
    public HandlerNotFoundException(Class<?> messageType) {
        super("No handler is registered for " + messageType.getName());
        this.messageType = messageType;
    }

    /**
     * El tipo del mensaje que no tiene handler.
     *
     * @return el tipo
     */
    public Class<?> messageType() {
        return messageType;
    }
}
