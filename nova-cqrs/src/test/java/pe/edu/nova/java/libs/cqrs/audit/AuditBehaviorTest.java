package pe.edu.nova.java.libs.cqrs.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import pe.edu.nova.java.libs.api.standard.error.ApplicationError;
import pe.edu.nova.java.libs.api.standard.error.DomainError;
import pe.edu.nova.java.libs.cqrs.ActorResolver;
import pe.edu.nova.java.libs.cqrs.Command;
import pe.edu.nova.java.libs.cqrs.MessageKind;
import pe.edu.nova.java.libs.cqrs.Query;

class AuditBehaviorTest {

    record Pay(String card) implements Command<String> {}

    record Balance(String account) implements Query<Long> {}

    private static final Instant START = Instant.parse("2026-10-02T15:00:00Z");

    private final List<AuditRecord> records = new ArrayList<>();
    private final ActorResolver alice = () -> Optional.of("alice");

    @Test
    void recordsWhoWhatWhenHowLongAndHowASuccessEnded() {
        AuditBehavior audit = new AuditBehavior(records::add, alice, new SteppingClock(START, 7), true);

        String result = audit.handle(new Pay("4111111111111111"), () -> "paid");

        assertThat(result).isEqualTo("paid");
        assertThat(records).singleElement().satisfies(entry -> {
            assertThat(entry.kind()).isEqualTo(MessageKind.COMMAND);
            assertThat(entry.messageType()).isEqualTo(Pay.class);
            assertThat(entry.actorIfAny()).contains("alice");
            assertThat(entry.startedAt()).isEqualTo(START);
            assertThat(entry.duration()).isEqualTo(Duration.ofMillis(7));
            assertThat(entry.outcome()).isEqualTo(AuditOutcome.SUCCEEDED);
            assertThat(entry.failureIfAny()).isEmpty();
        });
    }

    @Test
    void neverRecordsTheContentOfTheMessage() {
        AuditBehavior audit = new AuditBehavior(records::add, alice, Clock.systemUTC(), true);

        audit.handle(new Pay("4111111111111111"), () -> "paid");

        assertThat(records.getFirst().toString())
                .doesNotContain("4111111111111111")
                .doesNotContain("paid");
    }

    @Test
    void recordsADenialAndLetsItThrough() {
        AuditBehavior audit = new AuditBehavior(records::add, alice, Clock.systemUTC(), true);
        ApplicationError denied = ApplicationError.forbidden("No");

        assertThatThrownBy(() -> audit.handle(new Pay("x"), () -> {
                    throw denied;
                }))
                .isSameAs(denied);
        assertThat(records.getFirst().outcome()).isEqualTo(AuditOutcome.DENIED);
        assertThat(records.getFirst().failureIfAny()).contains("FORBIDDEN");
    }

    @Test
    void recordsAMissingIdentityAsADenial() {
        AuditBehavior audit = new AuditBehavior(records::add, ActorResolver.anonymous(), Clock.systemUTC(), true);

        assertThatThrownBy(() -> audit.handle(new Pay("x"), () -> {
            throw ApplicationError.unauthenticated("No");
        }));

        assertThat(records.getFirst().outcome()).isEqualTo(AuditOutcome.DENIED);
        assertThat(records.getFirst().actorIfAny()).isEmpty();
    }

    @Test
    void recordsTheCodeOfAFailureAndNeverItsText() {
        AuditBehavior audit = new AuditBehavior(records::add, alice, Clock.systemUTC(), true);

        assertThatThrownBy(() -> audit.handle(new Pay("x"), () -> {
            throw DomainError.notFound("CARD_NOT_FOUND", "La tarjeta 4111 no existe");
        }));
        assertThatThrownBy(() -> audit.handle(new Pay("x"), () -> {
            throw DomainError.conflict("La tarjeta 4111 está bloqueada");
        }));
        assertThatThrownBy(() -> audit.handle(new Pay("x"), () -> {
            throw new IllegalStateException("card 4111");
        }));

        assertThat(records).extracting(AuditRecord::outcome).containsOnly(AuditOutcome.FAILED);
        assertThat(records)
                .extracting(AuditRecord::failure)
                .containsExactly("CARD_NOT_FOUND", "CONFLICT", "IllegalStateException");
    }

    @Test
    void auditsQueriesToo() {
        AuditBehavior audit = new AuditBehavior(records::add, alice, Clock.systemUTC(), true);

        Long balance = audit.handle(new Balance("a-1"), () -> 10L);

        assertThat(balance).isEqualTo(10L);
        assertThat(records.getFirst().kind()).isEqualTo(MessageKind.QUERY);
        assertThat(records.getFirst().messageType()).isEqualTo(Balance.class);
    }

    @Test
    void leavesQueriesOutWhenTheServiceAsks() {
        AuditBehavior audit = new AuditBehavior(records::add, alice, Clock.systemUTC(), false);

        audit.handle(new Balance("a-1"), () -> 10L);
        audit.handle(new Pay("x"), () -> "paid");

        assertThat(records).extracting(AuditRecord::kind).containsExactly(MessageKind.COMMAND);
    }

    @Test
    void keepsTheResultWhenTheSinkFails() {
        AuditSink broken = entry -> {
            throw new IllegalStateException("sink down");
        };
        AuditBehavior audit = new AuditBehavior(broken, alice, Clock.systemUTC(), true);

        assertThat(audit.handle(new Pay("x"), () -> "paid")).isEqualTo("paid");
    }

    @Test
    void refusesARecordWithoutItsRequiredFields() {
        assertThatThrownBy(() -> new AuditRecord(
                        MessageKind.COMMAND, Pay.class, null, START, null, AuditOutcome.SUCCEEDED, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("duration");
    }

    /** Un reloj que avanza unos milisegundos cada vez que se le pregunta la hora. */
    private static final class SteppingClock extends Clock {

        private Instant now;
        private final long stepMillis;

        SteppingClock(Instant start, long stepMillis) {
            this.now = start;
            this.stepMillis = stepMillis;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            Instant current = now;
            now = now.plusMillis(stepMillis);
            return current;
        }
    }
}
