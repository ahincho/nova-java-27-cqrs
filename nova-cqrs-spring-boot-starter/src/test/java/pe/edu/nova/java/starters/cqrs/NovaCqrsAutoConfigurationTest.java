package pe.edu.nova.java.starters.cqrs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.nova.java.libs.api.standard.error.ApplicationError;
import pe.edu.nova.java.libs.api.standard.error.FieldError;
import pe.edu.nova.java.libs.cqrs.ActorResolver;
import pe.edu.nova.java.libs.cqrs.Command;
import pe.edu.nova.java.libs.cqrs.CommandBehavior;
import pe.edu.nova.java.libs.cqrs.CommandBus;
import pe.edu.nova.java.libs.cqrs.CommandHandler;
import pe.edu.nova.java.libs.cqrs.DuplicateHandlerException;
import pe.edu.nova.java.libs.cqrs.MessageKind;
import pe.edu.nova.java.libs.cqrs.Next;
import pe.edu.nova.java.libs.cqrs.QueryBus;
import pe.edu.nova.java.libs.cqrs.QueryHandler;
import pe.edu.nova.java.libs.cqrs.access.AccessPolicy;
import pe.edu.nova.java.libs.cqrs.access.AuthorizationBehavior;
import pe.edu.nova.java.libs.cqrs.audit.AuditBehavior;
import pe.edu.nova.java.libs.cqrs.audit.AuditOutcome;
import pe.edu.nova.java.libs.cqrs.audit.AuditRecord;
import pe.edu.nova.java.starters.cqrs.Orders.FindOrder;
import pe.edu.nova.java.starters.cqrs.Orders.PlaceOrder;
import pe.edu.nova.java.starters.cqrs.Orders.PlaceOrderHandler;
import pe.edu.nova.java.starters.cqrs.Orders.RecordingAuditSink;
import pe.edu.nova.java.starters.cqrs.Orders.RecordingTransactionManager;
import pe.edu.nova.java.starters.cqrs.Orders.TransactionalPlaceOrderHandler;

class NovaCqrsAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NovaCqrsAutoConfiguration.class))
            .withUserConfiguration(Handlers.class, Infrastructure.class);

    @AfterEach
    void clearTheSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void executesTheHandlersThatAreBeansWithTheirTypedResult() {
        runner.run(context -> {
            UUID id = context.getBean(CommandBus.class).execute(new PlaceOrder("c-1", 2));
            String order = context.getBean(QueryBus.class).execute(new FindOrder(Orders.PLACED));

            assertThat(id).isEqualTo(Orders.PLACED);
            assertThat(order).isEqualTo("order " + Orders.PLACED);
        });
    }

    @Test
    void findsTheMessageTypeBehindATransactionalProxy() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(NovaCqrsAutoConfiguration.class))
                .withUserConfiguration(TransactionalHandlers.class)
                .run(context -> {
                    assertThat(context.getBean(CommandHandler.class))
                            .isNotInstanceOf(TransactionalPlaceOrderHandler.class);
                    assertThat(context.getBean(CommandBus.class).execute(new PlaceOrder("c-1", 1)))
                            .isEqualTo(Orders.PLACED);
                });
    }

    @Test
    void refusesToStartWithTwoHandlersForOneMessage() {
        runner.withUserConfiguration(DuplicateHandler.class)
                .run(context -> assertThat(context)
                        .getFailure()
                        .rootCause()
                        .isInstanceOf(DuplicateHandlerException.class)
                        .hasMessageContaining(PlaceOrder.class.getName()));
    }

    @Test
    void refusesToStartWithAHandlerThatDoesNotSayWhatItHandles() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(NovaCqrsAutoConfiguration.class))
                .withUserConfiguration(RawHandler.class)
                .run(context -> assertThat(context)
                        .getFailure()
                        .rootCause()
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("'rawHandler'"));
    }

    @Test
    void ordersTheBehaviorsAsTheContractSays() {
        runner.run(context -> assertThat(behaviors(context.getBeanProvider(CommandBehavior.class)))
                .containsExactly(
                        ObservationBehavior.class,
                        AuditBehavior.class,
                        AuthorizationBehavior.class,
                        ValidationBehavior.class,
                        TransactionBehavior.class));
    }

    @Test
    void placesAServiceBehaviorBetweenNovaOnesByItsOrder() {
        runner.withUserConfiguration(CustomBehavior.class)
                .run(context -> assertThat(behaviors(context.getBeanProvider(CommandBehavior.class)))
                        .containsExactly(
                                ObservationBehavior.class,
                                AuditBehavior.class,
                                AuthorizationBehavior.class,
                                TenantBehavior.class,
                                ValidationBehavior.class,
                                TransactionBehavior.class));
    }

    @Test
    void auditsADenialBeforeValidatingOrOpeningATransaction() {
        runner.withBean(AccessPolicy.class, () -> (actor, message) -> false).run(context -> {
            CommandBus bus = context.getBean(CommandBus.class);

            assertThatThrownBy(() -> bus.execute(new PlaceOrder("", 0)))
                    .isInstanceOfSatisfying(
                            ApplicationError.class,
                            error -> assertThat(error.type()).isEqualTo(ApplicationError.Type.FORBIDDEN));
            assertThat(context.getBean(RecordingAuditSink.class).records)
                    .singleElement()
                    .extracting(AuditRecord::outcome)
                    .isEqualTo(AuditOutcome.DENIED);
            assertThat(context.getBean(RecordingTransactionManager.class).events)
                    .isEmpty();
        });
    }

    @Test
    void validatesEachFieldBeforeOpeningTheTransaction() {
        runner.run(context -> {
            CommandBus bus = context.getBean(CommandBus.class);

            assertThatThrownBy(() -> bus.execute(new PlaceOrder(" ", -1)))
                    .isInstanceOfSatisfying(ApplicationError.class, error -> {
                        assertThat(error.type()).isEqualTo(ApplicationError.Type.INVALID_INPUT);
                        assertThat(error.fieldErrors())
                                .extracting(FieldError::field)
                                .containsExactly("customerId", "quantity");
                    });
            assertThat(context.getBean(RecordingTransactionManager.class).events)
                    .isEmpty();
        });
    }

    @Test
    void runsACommandReadWriteAndAQueryReadOnly() {
        runner.run(context -> {
            context.getBean(CommandBus.class).execute(new PlaceOrder("c-1", 1));
            context.getBean(QueryBus.class).execute(new FindOrder(Orders.PLACED));

            assertThat(context.getBean(RecordingTransactionManager.class).events)
                    .containsExactly("begin:read-write", "commit", "begin:read-only", "commit");
        });
    }

    @Test
    void joinsATransactionThatIsAlreadyOpen() {
        runner.run(context -> {
            RecordingTransactionManager manager = context.getBean(RecordingTransactionManager.class);
            CommandBus bus = context.getBean(CommandBus.class);

            new TransactionTemplate(manager).execute(status -> bus.execute(new PlaceOrder("c-1", 1)));

            assertThat(manager.events).containsExactly("begin:read-write", "commit");
        });
    }

    @Test
    void rollsBackWhenTheHandlerFails() {
        runner.withUserConfiguration(FailingHandler.class).run(context -> {
            CommandBus bus = context.getBean(CommandBus.class);

            assertThatThrownBy(() -> bus.execute(new Cancel("o-1"))).hasMessage("out of stock");
            assertThat(context.getBean(RecordingTransactionManager.class).events)
                    .containsExactly("begin:read-write", "rollback");
        });
    }

    @Test
    void auditsCommandsAndQueriesByDefault() {
        runner.run(context -> {
            context.getBean(CommandBus.class).execute(new PlaceOrder("c-1", 1));
            context.getBean(QueryBus.class).execute(new FindOrder(Orders.PLACED));

            assertThat(context.getBean(RecordingAuditSink.class).records)
                    .extracting(AuditRecord::kind, AuditRecord::messageType, AuditRecord::outcome)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple(
                                    MessageKind.COMMAND, PlaceOrder.class, AuditOutcome.SUCCEEDED),
                            org.assertj.core.groups.Tuple.tuple(
                                    MessageKind.QUERY, FindOrder.class, AuditOutcome.SUCCEEDED));
        });
    }

    @Test
    void leavesQueriesOutOfTheAuditWhenTheServiceAsks() {
        runner.withPropertyValues("nova.cqrs.audit.queries=false").run(context -> {
            context.getBean(QueryBus.class).execute(new FindOrder(Orders.PLACED));

            assertThat(context.getBean(RecordingAuditSink.class).records).isEmpty();
        });
    }

    @Test
    void turnsEachBehaviorOff() {
        runner.withPropertyValues(
                        "nova.cqrs.observation.enabled=false",
                        "nova.cqrs.audit.enabled=false",
                        "nova.cqrs.authorization.enabled=false",
                        "nova.cqrs.validation.enabled=false",
                        "nova.cqrs.transaction.enabled=false")
                .run(context -> {
                    assertThat(behaviors(context.getBeanProvider(CommandBehavior.class)))
                            .isEmpty();
                    assertThat(context.getBean(CommandBus.class).execute(new PlaceOrder("", 0)))
                            .isEqualTo(Orders.PLACED);
                });
    }

    @Test
    void turnsTheWholeStarterOff() {
        runner.withPropertyValues("nova.cqrs.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(CommandBus.class));
    }

    @Test
    void takesTheActorFromTheServiceResolver() {
        runner.withBean(ActorResolver.class, () -> () -> Optional.of("customer-7"))
                .run(context -> {
                    context.getBean(CommandBus.class).execute(new PlaceOrder("c-1", 1));

                    assertThat(context.getBean(RecordingAuditSink.class)
                                    .records
                                    .getFirst()
                                    .actorIfAny())
                            .contains("customer-7");
                });
    }

    @Test
    void takesTheActorFromSpringSecurity() {
        runner.run(context -> {
            CommandBus bus = context.getBean(CommandBus.class);
            SecurityContextHolder.getContext()
                    .setAuthentication(UsernamePasswordAuthenticationToken.authenticated("alice", null, List.of()));
            bus.execute(new PlaceOrder("c-1", 1));
            SecurityContextHolder.getContext()
                    .setAuthentication(new AnonymousAuthenticationToken(
                            "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
            bus.execute(new PlaceOrder("c-1", 1));
            SecurityContextHolder.clearContext();
            bus.execute(new PlaceOrder("c-1", 1));

            assertThat(context.getBean(RecordingAuditSink.class).records)
                    .extracting(AuditRecord::actor)
                    .containsExactly("alice", null, null);
        });
    }

    @Test
    void observesEachMessageWithItsType() {
        TestObservationRegistry registry = TestObservationRegistry.create();
        runner.withBean(TestObservationRegistry.class, () -> registry).run(context -> {
            context.getBean(CommandBus.class).execute(new PlaceOrder("c-1", 1));
            context.getBean(QueryBus.class).execute(new FindOrder(Orders.PLACED));

            TestObservationRegistryAssert.assertThat(registry)
                    .hasObservationWithNameEqualTo(ObservationBehavior.COMMAND)
                    .that()
                    .hasContextualNameEqualTo("nova.cqrs.command PlaceOrder")
                    .hasLowCardinalityKeyValue("message", "PlaceOrder")
                    .hasLowCardinalityKeyValue("outcome", "success")
                    .hasBeenStopped();
            TestObservationRegistryAssert.assertThat(registry)
                    .hasObservationWithNameEqualTo(ObservationBehavior.QUERY)
                    .that()
                    .hasLowCardinalityKeyValue("message", "FindOrder");
        });
    }

    @Test
    void observesAFailureWithItsError() {
        TestObservationRegistry registry = TestObservationRegistry.create();
        runner.withBean(TestObservationRegistry.class, () -> registry)
                .withUserConfiguration(FailingHandler.class)
                .run(context -> {
                    CommandBus bus = context.getBean(CommandBus.class);
                    assertThatThrownBy(() -> bus.execute(new Cancel("o-1"))).hasMessage("out of stock");

                    TestObservationRegistryAssert.assertThat(registry)
                            .hasSingleObservationThat()
                            .hasNameEqualTo(ObservationBehavior.COMMAND)
                            .hasLowCardinalityKeyValue("message", "Cancel")
                            .hasLowCardinalityKeyValue("outcome", "failure")
                            .hasError()
                            .hasBeenStopped();
                });
    }

    private static List<Class<?>> behaviors(ObjectProvider<CommandBehavior> provider) {
        return provider.orderedStream().<Class<?>>map(Object::getClass).toList();
    }

    record Cancel(String orderId) implements Command<Void> {}

    @Configuration(proxyBeanMethods = false)
    static class Handlers {

        @Bean
        PlaceOrderHandler placeOrderHandler() {
            return new PlaceOrderHandler();
        }

        /** Una lambda: el tipo de mensaje sale del tipo que devuelve el método. */
        @Bean
        QueryHandler<FindOrder, String> findOrderHandler() {
            return query -> "order " + query.id();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class Infrastructure {

        @Bean
        RecordingAuditSink recordingAuditSink() {
            return new RecordingAuditSink();
        }

        @Bean
        RecordingTransactionManager recordingTransactionManager() {
            return new RecordingTransactionManager();
        }

        @Bean
        Validator validator() {
            return Validation.buildDefaultValidatorFactory().getValidator();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    static class TransactionalHandlers {

        @Bean
        TransactionalPlaceOrderHandler transactionalPlaceOrderHandler() {
            return new TransactionalPlaceOrderHandler();
        }

        @Bean
        RecordingTransactionManager recordingTransactionManager() {
            return new RecordingTransactionManager();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class DuplicateHandler {

        @Bean
        CommandHandler<PlaceOrder, UUID> anotherPlaceOrderHandler() {
            return command -> Orders.PLACED;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class RawHandler {

        @Bean
        @SuppressWarnings({"rawtypes", "unchecked"})
        CommandHandler rawHandler() {
            return command -> null;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class FailingHandler {

        @Bean
        CommandHandler<Cancel, Void> cancelHandler() {
            return command -> {
                throw new IllegalStateException("out of stock");
            };
        }
    }

    /** Un comportamiento del servicio, como el que pone el inquilino en el contexto. */
    static final class TenantBehavior implements CommandBehavior {
        @Override
        public <R> R handle(Command<R> command, Next<R> next) {
            return next.proceed();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomBehavior {

        @Bean
        @Order(350)
        TenantBehavior tenantBehavior() {
            return new TenantBehavior();
        }
    }
}
