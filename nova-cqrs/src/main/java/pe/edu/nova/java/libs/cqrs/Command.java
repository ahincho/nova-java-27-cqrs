package pe.edu.nova.java.libs.cqrs;

/**
 * Una intención de cambiar el estado, que el {@link CommandBus} entrega a su {@link CommandHandler} (ADR-053).
 *
 * <p>Un comando es un record inmutable, y su clase exacta es la que encuentra al handler. Puede devolver un valor,
 * y lo recomendado es el identificador de lo que creó; nunca la vista de lectura, que es trabajo de una
 * {@link Query}. Un comando sin resultado declara {@code Command<Void>}.
 *
 * <pre>{@code
 * public record PlaceOrder(String customerId, List<Line> lines) implements Command<UUID> {}
 *
 * UUID id = commandBus.execute(new PlaceOrder(customerId, lines));
 * }</pre>
 *
 * @param <R> el tipo del resultado
 */
public interface Command<R> extends Message<R> {}
