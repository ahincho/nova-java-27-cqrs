package pe.edu.nova.java.libs.cqrs;

/**
 * Un paso de la cadena del {@link QueryBus}, alrededor de cada handler (ADR-053).
 *
 * <p>Recibe la consulta y el resto de la cadena, y decide: llama a {@link Next#proceed()} y devuelve su
 * resultado, o corta la cadena lanzando una excepción. Un comportamiento no cambia el resultado del handler.
 */
public interface QueryBehavior {

    /**
     * Aplica el comportamiento a la consulta.
     *
     * @param query la consulta
     * @param next  el resto de la cadena
     * @param <R>   el tipo del resultado
     * @return lo que devolvió el resto de la cadena
     */
    <R> R handle(Query<R> query, Next<R> next);
}
