package pe.edu.nova.java.starters.cqrs;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.observation.ObservationRegistry;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.transaction.PlatformTransactionManager;
import pe.edu.nova.java.starters.cqrs.Orders.FindOrder;
import pe.edu.nova.java.starters.cqrs.Orders.PlaceOrder;

/** Sin la pieza que usa, cada comportamiento deja pasar el mensaje como está. */
class OptionalInfrastructureTest {

    private final DefaultListableBeanFactory empty = new DefaultListableBeanFactory();

    @Test
    void validationLetsTheMessageThroughWithoutAValidator() {
        ValidationBehavior validation = new ValidationBehavior(empty.getBeanProvider(Validator.class));

        assertThat(validation.handle(new PlaceOrder("", -1), () -> Orders.PLACED))
                .isEqualTo(Orders.PLACED);
    }

    @Test
    void theTransactionIsSkippedWithoutATransactionManager() {
        TransactionBehavior transaction =
                new TransactionBehavior(empty.getBeanProvider(PlatformTransactionManager.class));

        assertThat(transaction.handle(new PlaceOrder("c-1", 1), () -> Orders.PLACED))
                .isEqualTo(Orders.PLACED);
        assertThat(transaction.handle(new FindOrder(Orders.PLACED), () -> "order"))
                .isEqualTo("order");
    }

    @Test
    void theObservationIsANoOpWithoutARegistry() {
        ObservationBehavior observation = new ObservationBehavior(empty.getBeanProvider(ObservationRegistry.class));

        assertThat(observation.handle(new FindOrder(Orders.PLACED), () -> "order"))
                .isEqualTo("order");
    }
}
