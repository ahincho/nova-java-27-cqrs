package pe.edu.nova.java.libs.cqrs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import pe.edu.nova.java.libs.cqrs.Messages.FindOrder;
import pe.edu.nova.java.libs.cqrs.Messages.FindOrderHandler;
import pe.edu.nova.java.libs.cqrs.Messages.ListOrders;
import pe.edu.nova.java.libs.cqrs.Messages.ListOrdersHandler;

class QueryBusTest {

    private final HandlerRegistry registry = HandlerRegistry.builder()
            .query(new FindOrderHandler())
            .query(new ListOrdersHandler())
            .build();

    @Test
    void returnsTheResultTypedByTheQuery() {
        QueryBus bus = new SimpleQueryBus(registry, List.of());

        String order = bus.execute(new FindOrder(Messages.PLACED));
        List<String> orders = bus.execute(new ListOrders("c-1"));

        assertThat(order).isEqualTo("order " + Messages.PLACED);
        assertThat(orders).containsExactly("a", "b");
    }

    @Test
    void runsTheBehaviorsInOrderAroundTheHandler() {
        List<String> trace = new ArrayList<>();
        QueryBus bus = new SimpleQueryBus(registry, List.of(tracer("outer", trace), tracer("inner", trace)));

        bus.execute(new FindOrder(Messages.PLACED));

        assertThat(trace).containsExactly("outer:before", "inner:before", "inner:after", "outer:after");
    }

    @Test
    void failsWhenNoHandlerAnswersTheQuery() {
        QueryBus bus = new SimpleQueryBus(HandlerRegistry.builder().build(), List.of());

        assertThatThrownBy(() -> bus.execute(new FindOrder(Messages.PLACED)))
                .isInstanceOf(HandlerNotFoundException.class)
                .hasMessageContaining(FindOrder.class.getName());
    }

    @Test
    void rejectsANullQuery() {
        QueryBus bus = new SimpleQueryBus(registry, List.of());

        assertThatThrownBy(() -> bus.execute(null)).isInstanceOf(NullPointerException.class);
    }

    private static QueryBehavior tracer(String name, List<String> trace) {
        return new QueryBehavior() {
            @Override
            public <R> R handle(Query<R> query, Next<R> next) {
                trace.add(name + ":before");
                R result = next.proceed();
                trace.add(name + ":after");
                return result;
            }
        };
    }
}
