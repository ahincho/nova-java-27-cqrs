package pe.edu.nova.java.libs.cqrs;

/**
 * Ejecuta un tipo de {@link Command}. Cada tipo de comando tiene exactamente uno (ADR-053).
 *
 * <p>Un handler no conoce el bus ni los comportamientos: recibe el comando y devuelve su resultado, así que se
 * prueba con un {@code new} y una llamada. Lo que lanza sale tal cual del bus, y un error de ADR-031 llega al
 * manejo de errores con su capa y su código.
 *
 * @param <C> el tipo del comando
 * @param <R> el tipo del resultado, el mismo que declara el comando
 */
@FunctionalInterface
public interface CommandHandler<C extends Command<R>, R> {

    /**
     * Ejecuta el comando.
     *
     * @param command el comando; nunca es {@code null}
     * @return el resultado, que puede ser {@code null} en un {@code Command<Void>}
     */
    R handle(C command);
}
