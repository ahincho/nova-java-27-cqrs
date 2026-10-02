package pe.edu.nova.java.libs.cqrs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.edu.nova.java.libs.cqrs.Messages.CancelOrder;
import pe.edu.nova.java.libs.cqrs.Messages.PlaceOrder;
import pe.edu.nova.java.libs.cqrs.Messages.PlaceOrderHandler;
import pe.edu.nova.java.libs.cqrs.Messages.Unhandled;

class CommandBusTest {

    private final HandlerRegistry registry = HandlerRegistry.builder()
            .command(new PlaceOrderHandler())
            .command(CancelOrder.class, command -> null)
            .build();

    @Test
    void returnsTheResultTypedByTheCommand() {
        CommandBus bus = new SimpleCommandBus(registry, List.of());

        UUID id = bus.execute(new PlaceOrder("c-1", 2));

        assertThat(id).isEqualTo(Messages.PLACED);
    }

    @Test
    void runsACommandWithoutResult() {
        CommandBus bus = new SimpleCommandBus(registry, List.of());

        Void nothing = bus.execute(new CancelOrder(Messages.PLACED));

        assertThat(nothing).isNull();
    }

    @Test
    void runsTheBehaviorsInOrderAroundTheHandler() {
        List<String> trace = new ArrayList<>();
        CommandHandler<PlaceOrder, UUID> handler = command -> {
            trace.add("handler");
            return Messages.PLACED;
        };
        HandlerRegistry tracing =
                HandlerRegistry.builder().command(PlaceOrder.class, handler).build();
        CommandBus bus = new SimpleCommandBus(tracing, List.of(tracer("outer", trace), tracer("inner", trace)));

        bus.execute(new PlaceOrder("c-1", 1));

        assertThat(trace).containsExactly("outer:before", "inner:before", "handler", "inner:after", "outer:after");
    }

    @Test
    void aBehaviorCanStopTheChain() {
        List<String> trace = new ArrayList<>();
        CommandBehavior refuse = new CommandBehavior() {
            @Override
            public <R> R handle(Command<R> command, Next<R> next) {
                throw new IllegalStateException("refused");
            }
        };
        CommandBus bus = new SimpleCommandBus(registry, List.of(refuse, tracer("inner", trace)));

        assertThatThrownBy(() -> bus.execute(new PlaceOrder("c-1", 1))).hasMessage("refused");
        assertThat(trace).isEmpty();
    }

    @Test
    void letsTheHandlerExceptionThroughUnchanged() {
        IllegalArgumentException thrown = new IllegalArgumentException("out of stock");
        HandlerRegistry failing = HandlerRegistry.builder()
                .command(PlaceOrder.class, command -> {
                    throw thrown;
                })
                .build();
        CommandBus bus = new SimpleCommandBus(failing, List.of(tracer("outer", new ArrayList<>())));

        assertThatThrownBy(() -> bus.execute(new PlaceOrder("c-1", 1))).isSameAs(thrown);
    }

    @Test
    void failsBeforeTheBehaviorsWhenNoHandlerExecutesTheCommand() {
        List<String> trace = new ArrayList<>();
        CommandBus bus = new SimpleCommandBus(registry, List.of(tracer("outer", trace)));

        assertThatThrownBy(() -> bus.execute(new Unhandled()))
                .isInstanceOf(HandlerNotFoundException.class)
                .hasMessageContaining(Unhandled.class.getName())
                .satisfies(e ->
                        assertThat(((HandlerNotFoundException) e).messageType()).isEqualTo(Unhandled.class));
        assertThat(trace).isEmpty();
    }

    @Test
    void rejectsANullCommand() {
        CommandBus bus = new SimpleCommandBus(registry, List.of());

        assertThatThrownBy(() -> bus.execute(null)).isInstanceOf(NullPointerException.class);
    }

    private static CommandBehavior tracer(String name, List<String> trace) {
        return new CommandBehavior() {
            @Override
            public <R> R handle(Command<R> command, Next<R> next) {
                trace.add(name + ":before");
                R result = next.proceed();
                trace.add(name + ":after");
                return result;
            }
        };
    }
}
