package pe.edu.nova.java.libs.cqrs;

/**
 * Recibe un {@link Command}, encuentra su {@link CommandHandler} y lo ejecuta a través de la cadena de
 * {@link CommandBehavior} (ADR-053).
 *
 * <p>Es síncrono y corre en el hilo de quien llama. El resultado sale del tipo del comando: {@code execute} de un
 * {@code Command<UUID>} devuelve un {@code UUID}, sin cast.
 */
public interface CommandBus {

    /**
     * Ejecuta el comando.
     *
     * @param command el comando
     * @param <R>     el tipo del resultado, el que declara el comando
     * @return lo que devolvió su handler
     * @throws NullPointerException     si el comando es {@code null}
     * @throws HandlerNotFoundException si ningún handler ejecuta ese tipo de comando
     */
    <R> R execute(Command<R> command);
}
