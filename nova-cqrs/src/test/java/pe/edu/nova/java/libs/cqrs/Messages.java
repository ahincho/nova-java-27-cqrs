package pe.edu.nova.java.libs.cqrs;

import java.util.List;
import java.util.UUID;

/** Los mensajes y handlers de las pruebas: un caso de pedidos, como el primer consumidor de ADR-053. */
final class Messages {

    private Messages() {}

    record PlaceOrder(String customerId, int quantity) implements Command<UUID> {}

    record CancelOrder(UUID id) implements Command<Void> {}

    record FindOrder(UUID id) implements Query<String> {}

    record ListOrders(String customerId) implements Query<List<String>> {}

    /** Un comando que nadie registra. */
    record Unhandled() implements Command<String> {}

    static final UUID PLACED = UUID.fromString("7f8f2a1c-6c1b-4e3a-9d2e-1b2c3d4e5f60");

    static final class PlaceOrderHandler implements CommandHandler<PlaceOrder, UUID> {
        @Override
        public UUID handle(PlaceOrder command) {
            return PLACED;
        }
    }

    static final class FindOrderHandler implements QueryHandler<FindOrder, String> {
        @Override
        public String handle(FindOrder query) {
            return "order " + query.id();
        }
    }

    /** Una base genérica, como la que una organización escribe para sus handlers. */
    abstract static class BaseQueryHandler<Q extends Query<R>, R> implements QueryHandler<Q, R> {}

    static final class ListOrdersHandler extends BaseQueryHandler<ListOrders, List<String>> {
        @Override
        public List<String> handle(ListOrders query) {
            return List.of("a", "b");
        }
    }

    /** Implementa la interfaz sin genéricos: no queda escrito qué comando ejecuta. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static final class RawHandler implements CommandHandler {
        @Override
        public Object handle(Command command) {
            return null;
        }
    }
}
