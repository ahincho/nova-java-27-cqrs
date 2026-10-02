package pe.edu.nova.java.libs.cqrs;

/**
 * Una pregunta que no cambia el estado, que el {@link QueryBus} entrega a su {@link QueryHandler} (ADR-053).
 *
 * <p>Una consulta es un record inmutable, y su clase exacta es la que encuentra al handler. Con transacciones,
 * corre de solo lectura: una escritura dentro de un {@link QueryHandler} falla en vez de pasar.
 *
 * <pre>{@code
 * public record FindOrder(String customerId, UUID id) implements Query<OrderView> {}
 *
 * OrderView order = queryBus.execute(new FindOrder(customerId, id));
 * }</pre>
 *
 * @param <R> el tipo del resultado
 */
public interface Query<R> extends Message<R> {}
