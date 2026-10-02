package pe.edu.nova.java.libs.cqrs.audit;

import java.lang.System.Logger.Level;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import pe.edu.nova.java.libs.api.standard.error.ApplicationError;
import pe.edu.nova.java.libs.api.standard.error.NovaError;
import pe.edu.nova.java.libs.cqrs.ActorResolver;
import pe.edu.nova.java.libs.cqrs.Command;
import pe.edu.nova.java.libs.cqrs.CommandBehavior;
import pe.edu.nova.java.libs.cqrs.Message;
import pe.edu.nova.java.libs.cqrs.MessageKind;
import pe.edu.nova.java.libs.cqrs.Next;
import pe.edu.nova.java.libs.cqrs.Query;
import pe.edu.nova.java.libs.cqrs.QueryBehavior;

/**
 * Registra cada mensaje en el {@link AuditSink}: quién, qué tipo, cuándo, cuánto tardó y cómo terminó (ADR-053).
 *
 * <p>Va antes que la autorización, así que un intento rechazado también queda registrado, como
 * {@link AuditOutcome#DENIED}. Audita comandos y consultas; un servicio puede dejar fuera las consultas.
 */
public final class AuditBehavior implements CommandBehavior, QueryBehavior {

    private static final System.Logger LOG = System.getLogger(AuditBehavior.class.getName());

    private final AuditSink sink;
    private final ActorResolver actors;
    private final Clock clock;
    private final boolean auditQueries;

    /**
     * Crea el comportamiento.
     *
     * @param sink         dónde van los registros
     * @param actors       de dónde sale el actor
     * @param clock        el reloj del instante y la duración
     * @param auditQueries si también se auditan las consultas
     */
    public AuditBehavior(AuditSink sink, ActorResolver actors, Clock clock, boolean auditQueries) {
        this.sink = Objects.requireNonNull(sink, "sink");
        this.actors = Objects.requireNonNull(actors, "actors");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.auditQueries = auditQueries;
    }

    @Override
    public <R> R handle(Command<R> command, Next<R> next) {
        return audit(command, next);
    }

    @Override
    public <R> R handle(Query<R> query, Next<R> next) {
        return auditQueries ? audit(query, next) : next.proceed();
    }

    private <R> R audit(Message<R> message, Next<R> next) {
        String actor = actors.currentActor().orElse(null);
        Instant startedAt = clock.instant();
        R result;
        try {
            result = next.proceed();
        } catch (RuntimeException failure) {
            record(message, actor, startedAt, outcomeOf(failure), codeOf(failure));
            throw failure;
        }
        record(message, actor, startedAt, AuditOutcome.SUCCEEDED, null);
        return result;
    }

    private void record(Message<?> message, String actor, Instant startedAt, AuditOutcome outcome, String failure) {
        Duration duration = Duration.between(startedAt, clock.instant());
        AuditRecord entry = new AuditRecord(
                MessageKind.of(message), message.getClass(), actor, startedAt, duration, outcome, failure);
        try {
            sink.record(entry);
        } catch (RuntimeException sinkFailure) {
            // El mensaje ya terminó: un registro que falla no cambia su resultado, pero no se pierde sin aviso
            LOG.log(
                    Level.ERROR,
                    "The audit sink failed to record " + message.getClass().getName(),
                    sinkFailure);
        }
    }

    private static AuditOutcome outcomeOf(RuntimeException failure) {
        if (failure instanceof ApplicationError error
                && (error.type() == ApplicationError.Type.FORBIDDEN
                        || error.type() == ApplicationError.Type.UNAUTHENTICATED)) {
            return AuditOutcome.DENIED;
        }
        return AuditOutcome.FAILED;
    }

    /** El código del error de ADR-031, o el nombre de la excepción; nunca su texto, que puede llevar datos. */
    private static String codeOf(RuntimeException failure) {
        if (failure instanceof NovaError error) {
            return error.code().orElse(error.type().toString());
        }
        return failure.getClass().getSimpleName();
    }
}
