package pe.edu.nova.java.starters.cqrs;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import pe.edu.nova.java.libs.cqrs.Command;
import pe.edu.nova.java.libs.cqrs.CommandBehavior;
import pe.edu.nova.java.libs.cqrs.Message;
import pe.edu.nova.java.libs.cqrs.Next;
import pe.edu.nova.java.libs.cqrs.Query;
import pe.edu.nova.java.libs.cqrs.QueryBehavior;

/**
 * Una {@link Observation} de Micrometer por mensaje: con el starter de observabilidad de Nova da el timer
 * {@code nova.cqrs.command} o {@code nova.cqrs.query} y un span en la traza (ADR-053).
 *
 * <p>Lleva dos etiquetas de baja cardinalidad: el tipo del mensaje y cómo terminó. El contenido del mensaje no
 * aparece en ninguna.
 */
public final class ObservationBehavior implements CommandBehavior, QueryBehavior {

    /** El nombre de la observación de un comando. */
    public static final String COMMAND = "nova.cqrs.command";

    /** El nombre de la observación de una consulta. */
    public static final String QUERY = "nova.cqrs.query";

    /** La etiqueta con el nombre simple del tipo del mensaje. */
    public static final String MESSAGE_KEY = "message";

    /** La etiqueta con el resultado: {@code success} o {@code failure}. */
    public static final String OUTCOME_KEY = "outcome";

    private final ObjectProvider<ObservationRegistry> registries;

    /**
     * Crea el comportamiento. El registro se busca al ejecutar, así que no depende del orden de la configuración.
     *
     * @param registries el registro de observaciones del servicio, si tiene uno
     */
    public ObservationBehavior(ObjectProvider<ObservationRegistry> registries) {
        this.registries = Objects.requireNonNull(registries, "registries");
    }

    @Override
    public <R> R handle(Command<R> command, Next<R> next) {
        return observe(COMMAND, command, next);
    }

    @Override
    public <R> R handle(Query<R> query, Next<R> next) {
        return observe(QUERY, query, next);
    }

    private <R> R observe(String name, Message<R> message, Next<R> next) {
        ObservationRegistry registry = registries.getIfAvailable(() -> ObservationRegistry.NOOP);
        String type = message.getClass().getSimpleName();
        Observation observation = Observation.createNotStarted(name, registry)
                .contextualName(name + " " + type)
                .lowCardinalityKeyValue(MESSAGE_KEY, type);
        observation.start();
        // El scope deja la observación como la actual, para que el span del handler cuelgue de ella
        Observation.Scope scope = observation.openScope();
        try {
            R result = next.proceed();
            observation.lowCardinalityKeyValue(OUTCOME_KEY, "success");
            return result;
        } catch (RuntimeException failure) {
            observation.lowCardinalityKeyValue(OUTCOME_KEY, "failure");
            observation.error(failure);
            throw failure;
        } finally {
            scope.close();
            observation.stop();
        }
    }
}
