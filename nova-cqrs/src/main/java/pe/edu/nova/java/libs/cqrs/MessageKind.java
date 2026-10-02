package pe.edu.nova.java.libs.cqrs;

/** Si un mensaje es un comando o una consulta. Lo usan los comportamientos que tratan distinto a cada uno. */
public enum MessageKind {

    /** Un {@link Command}. */
    COMMAND,

    /** Una {@link Query}. */
    QUERY;

    /**
     * El tipo de un mensaje.
     *
     * @param message el mensaje
     * @return {@link #COMMAND} o {@link #QUERY}
     * @throws IllegalArgumentException si el mensaje no es ni un comando ni una consulta
     */
    public static MessageKind of(Message<?> message) {
        if (message instanceof Command<?>) {
            return COMMAND;
        }
        if (message instanceof Query<?>) {
            return QUERY;
        }
        throw new IllegalArgumentException(
                "A message must be a Command or a Query: " + message.getClass().getName());
    }
}
