package pe.edu.nova.java.libs.cqrs.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import pe.edu.nova.java.libs.api.standard.error.ApplicationError;
import pe.edu.nova.java.libs.cqrs.ActorResolver;
import pe.edu.nova.java.libs.cqrs.Command;
import pe.edu.nova.java.libs.cqrs.Message;
import pe.edu.nova.java.libs.cqrs.Query;

class AuthorizationBehaviorTest {

    record Refund(String orderId) implements Command<Void> {}

    record Report(String month) implements Query<String> {}

    @Test
    void letsThroughWhatThePolicyPermits() {
        AuthorizationBehavior authorization =
                new AuthorizationBehavior(AccessPolicy.permitAll(), ActorResolver.anonymous());

        assertThat(authorization.handle(new Report("2026-10"), () -> "ok")).isEqualTo("ok");
    }

    @Test
    void stopsWhatThePolicyDeniesBeforeTheHandlerRuns() {
        AccessPolicy adminsOnly =
                (actor, message) -> actor.filter("admin"::equals).isPresent();
        AuthorizationBehavior authorization = new AuthorizationBehavior(adminsOnly, () -> Optional.of("bob"));
        AtomicBoolean ran = new AtomicBoolean();

        assertThatThrownBy(() -> authorization.handle(new Refund("o-1"), () -> {
                    ran.set(true);
                    return null;
                }))
                .isInstanceOfSatisfying(
                        ApplicationError.class,
                        error -> assertThat(error.type()).isEqualTo(ApplicationError.Type.FORBIDDEN));
        assertThat(ran).isFalse();
    }

    @Test
    void givesThePolicyTheActorAndTheMessage() {
        List<Object> seen = new ArrayList<>();
        AccessPolicy recording = (actor, message) -> {
            seen.add(actor);
            seen.add(message);
            return true;
        };
        Message<String> report = new Report("2026-10");
        AuthorizationBehavior authorization = new AuthorizationBehavior(recording, () -> Optional.of("alice"));

        authorization.handle(new Report("2026-10"), () -> "ok");

        assertThat(seen).containsExactly(Optional.of("alice"), report);
    }
}
