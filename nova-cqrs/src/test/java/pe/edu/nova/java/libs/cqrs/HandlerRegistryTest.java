package pe.edu.nova.java.libs.cqrs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.edu.nova.java.libs.cqrs.Messages.FindOrder;
import pe.edu.nova.java.libs.cqrs.Messages.FindOrderHandler;
import pe.edu.nova.java.libs.cqrs.Messages.ListOrders;
import pe.edu.nova.java.libs.cqrs.Messages.ListOrdersHandler;
import pe.edu.nova.java.libs.cqrs.Messages.PlaceOrder;
import pe.edu.nova.java.libs.cqrs.Messages.PlaceOrderHandler;
import pe.edu.nova.java.libs.cqrs.Messages.RawHandler;

class HandlerRegistryTest {

    @Test
    void readsTheMessageTypeFromTheHandlerGenerics() {
        HandlerRegistry registry = HandlerRegistry.builder()
                .command(new PlaceOrderHandler())
                .query(new FindOrderHandler())
                .build();

        assertThat(registry.commandTypes()).containsExactly(PlaceOrder.class);
        assertThat(registry.queryTypes()).containsExactly(FindOrder.class);
    }

    @Test
    void readsTheMessageTypeThroughAGenericBaseClass() {
        HandlerRegistry registry =
                HandlerRegistry.builder().query(new ListOrdersHandler()).build();

        assertThat(registry.queryTypes()).containsExactly(ListOrders.class);
    }

    @Test
    void registersALambdaWithItsMessageType() {
        CommandHandler<PlaceOrder, UUID> handler = command -> Messages.PLACED;
        HandlerRegistry registry =
                HandlerRegistry.builder().command(PlaceOrder.class, handler).build();

        assertThat(registry.commandHandlerFor(new PlaceOrder("c-1", 1))).isSameAs(handler);
    }

    @Test
    void refusesALambdaWithoutItsMessageType() {
        CommandHandler<PlaceOrder, UUID> handler = command -> Messages.PLACED;

        assertThatThrownBy(() -> HandlerRegistry.builder().command(handler))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("command(PlaceOrder.class, handler)");
    }

    @Test
    void refusesARawHandler() {
        assertThatThrownBy(() -> HandlerRegistry.builder().command(new RawHandler()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(RawHandler.class.getName());
    }

    @Test
    void refusesTwoHandlersForTheSameMessage() {
        HandlerRegistry.Builder builder = HandlerRegistry.builder().command(new PlaceOrderHandler());

        assertThatThrownBy(() -> builder.command(PlaceOrder.class, command -> Messages.PLACED))
                .isInstanceOf(DuplicateHandlerException.class)
                .hasMessageContaining(PlaceOrder.class.getName())
                .hasMessageContaining(PlaceOrderHandler.class.getName());
    }

    /** Una consulta declarada como interfaz: ningún mensaje tiene esa clase exacta. */
    interface Lookup extends Query<String> {}

    @Test
    void refusesAMessageTypeThatIsNotAConcreteClass() {
        assertThatThrownBy(() -> HandlerRegistry.builder().query(Lookup.class, query -> "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("concrete class");
    }

    @Test
    void findsAHandlerByTheExactClassOnly() {
        HandlerRegistry registry = HandlerRegistry.builder().build();

        assertThatThrownBy(() -> registry.queryHandlerFor(new FindOrder(Messages.PLACED)))
                .isInstanceOf(HandlerNotFoundException.class);
        assertThatThrownBy(() -> registry.commandHandlerFor(new PlaceOrder("c-1", 1)))
                .isInstanceOf(HandlerNotFoundException.class);
    }

    @Test
    void tellsACommandFromAQuery() {
        assertThat(MessageKind.of(new PlaceOrder("c-1", 1))).isEqualTo(MessageKind.COMMAND);
        assertThat(MessageKind.of(new FindOrder(Messages.PLACED))).isEqualTo(MessageKind.QUERY);
        assertThatThrownBy(() -> MessageKind.of(new Message<String>() {})).isInstanceOf(IllegalArgumentException.class);
    }
}
