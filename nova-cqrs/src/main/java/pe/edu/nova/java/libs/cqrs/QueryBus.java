package pe.edu.nova.java.libs.cqrs;

/**
 * Recibe una {@link Query}, encuentra su {@link QueryHandler} y la responde a través de la cadena de
 * {@link QueryBehavior} (ADR-053).
 *
 * <p>Es síncrono y corre en el hilo de quien llama. El resultado sale del tipo de la consulta: {@code execute} de
 * una {@code Query<OrderView>} devuelve un {@code OrderView}, sin cast.
 */
public interface QueryBus {

    /**
     * Responde la consulta.
     *
     * @param query la consulta
     * @param <R>   el tipo del resultado, el que declara la consulta
     * @return lo que devolvió su handler
     * @throws NullPointerException     si la consulta es {@code null}
     * @throws HandlerNotFoundException si ningún handler responde ese tipo de consulta
     */
    <R> R execute(Query<R> query);
}
