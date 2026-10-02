package pe.edu.nova.java.starters.cqrs;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.event.KeyValuePair;
import pe.edu.nova.java.libs.cqrs.MessageKind;
import pe.edu.nova.java.libs.cqrs.audit.AuditOutcome;
import pe.edu.nova.java.libs.cqrs.audit.AuditRecord;
import pe.edu.nova.java.starters.cqrs.Orders.PlaceOrder;

class LoggingAuditSinkTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(LoggingAuditSink.LOGGER);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void attach() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detach() {
        logger.detachAppender(appender);
    }

    @Test
    void writesOneLineWithEachFieldAlsoAsAKeyValue() {
        new LoggingAuditSink()
                .record(new AuditRecord(
                        MessageKind.COMMAND,
                        PlaceOrder.class,
                        "alice",
                        Instant.parse("2026-10-02T15:00:00Z"),
                        Duration.ofMillis(12),
                        AuditOutcome.FAILED,
                        "OUT_OF_STOCK"));

        assertThat(appender.list).singleElement().satisfies(event -> {
            assertThat(event.getFormattedMessage())
                    .isEqualTo("COMMAND PlaceOrder by alice: FAILED OUT_OF_STOCK in 12 ms");
            assertThat(event.getKeyValuePairs())
                    .extracting(pair -> pair.key)
                    .containsExactly(
                            "audit.kind",
                            "audit.message",
                            "audit.actor",
                            "audit.startedAt",
                            "audit.durationMs",
                            "audit.outcome",
                            "audit.failure");
            assertThat(event.getKeyValuePairs())
                    .filteredOn(pair -> pair.key.equals("audit.message"))
                    .extracting(KeyValuePair::toString)
                    .singleElement()
                    .asString()
                    .contains(PlaceOrder.class.getName());
        });
    }

    @Test
    void writesADashWhenThereIsNoActorOrFailure() {
        new LoggingAuditSink()
                .record(new AuditRecord(
                        MessageKind.QUERY,
                        PlaceOrder.class,
                        null,
                        Instant.EPOCH,
                        Duration.ZERO,
                        AuditOutcome.SUCCEEDED,
                        null));

        assertThat(appender.list.getFirst().getFormattedMessage())
                .isEqualTo("QUERY PlaceOrder by -: SUCCEEDED - in 0 ms");
    }
}
