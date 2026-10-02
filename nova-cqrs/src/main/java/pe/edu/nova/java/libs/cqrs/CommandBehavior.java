package pe.edu.nova.java.libs.cqrs;

/**
 * Un paso de la cadena del {@link CommandBus}, alrededor de cada handler (ADR-053).
 *
 * <p>Recibe el comando y el resto de la cadena, y decide: llama a {@link Next#proceed()} y devuelve su resultado,
 * o corta la cadena lanzando una excepción. Es donde vive lo transversal: la observación, la auditoría, la
 * autorización, la validación y la transacción. Un comportamiento no cambia el resultado del handler.
 */
public interface CommandBehavior {

    /**
     * Aplica el comportamiento al comando.
     *
     * @param command el comando
     * @param next    el resto de la cadena
     * @param <R>     el tipo del resultado
     * @return lo que devolvió el resto de la cadena
     */
    <R> R handle(Command<R> command, Next<R> next);
}
