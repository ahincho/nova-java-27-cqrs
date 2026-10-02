package pe.edu.nova.java.libs.cqrs;

import java.util.List;
import java.util.Objects;

/**
 * El {@link QueryBus} de Nova: busca el handler en un {@link HandlerRegistry} y lo ejecuta dentro de la cadena de
 * comportamientos, en el orden en que llegan, el primero por fuera de todos.
 *
 * <p>El handler se busca antes de entrar en la cadena, así que una consulta sin handler falla sin pasar por los
 * comportamientos.
 */
public final class SimpleQueryBus implements QueryBus {

    private final HandlerRegistry registry;
    private final List<QueryBehavior> behaviors;

    /**
     * Crea el bus.
     *
     * @param registry  los handlers
     * @param behaviors los comportamientos, de afuera hacia adentro
     */
    public SimpleQueryBus(HandlerRegistry registry, List<? extends QueryBehavior> behaviors) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.behaviors = List.copyOf(behaviors);
    }

    @Override
    public <R> R execute(Query<R> query) {
        Objects.requireNonNull(query, "query");
        QueryHandler<Query<R>, R> handler = registry.queryHandlerFor(query);
        return proceed(0, query, handler);
    }

    private <R> R proceed(int index, Query<R> query, QueryHandler<Query<R>, R> handler) {
        if (index == behaviors.size()) {
            return handler.handle(query);
        }
        return behaviors.get(index).handle(query, () -> proceed(index + 1, query, handler));
    }
}
