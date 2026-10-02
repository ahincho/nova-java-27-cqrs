package pe.edu.nova.java.libs.cqrs;

/**
 * Responde un tipo de {@link Query}. Cada tipo de consulta tiene exactamente uno (ADR-053).
 *
 * <p>Un handler no conoce el bus ni los comportamientos, y no cambia el estado. Lo que lanza sale tal cual del
 * bus.
 *
 * @param <Q> el tipo de la consulta
 * @param <R> el tipo del resultado, el mismo que declara la consulta
 */
@FunctionalInterface
public interface QueryHandler<Q extends Query<R>, R> {

    /**
     * Responde la consulta.
     *
     * @param query la consulta; nunca es {@code null}
     * @return el resultado
     */
    R handle(Q query);
}
