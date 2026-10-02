package pe.edu.nova.java.libs.cqrs.access;

import java.util.Objects;
import pe.edu.nova.java.libs.api.standard.error.ApplicationError;
import pe.edu.nova.java.libs.cqrs.ActorResolver;
import pe.edu.nova.java.libs.cqrs.Command;
import pe.edu.nova.java.libs.cqrs.CommandBehavior;
import pe.edu.nova.java.libs.cqrs.Message;
import pe.edu.nova.java.libs.cqrs.Next;
import pe.edu.nova.java.libs.cqrs.Query;
import pe.edu.nova.java.libs.cqrs.QueryBehavior;

/**
 * Pregunta a la {@link AccessPolicy} si el actor puede ejecutar el mensaje, antes de validarlo y de abrir la
 * transacción (ADR-053).
 *
 * <p>Si no puede, corta la cadena con un {@link ApplicationError} de tipo {@code FORBIDDEN}, que el estándar de
 * API responde como un 403. Aplica igual a comandos y a consultas.
 */
public final class AuthorizationBehavior implements CommandBehavior, QueryBehavior {

    /**
     * El mensaje para la persona, el mismo del catálogo de Nova para un 403. No dice qué operación era ni por qué:
     * eso queda en la auditoría.
     */
    static final String DENIED = "No hay permiso para esta operación";

    private final AccessPolicy policy;
    private final ActorResolver actors;

    /**
     * Crea el comportamiento.
     *
     * @param policy la política de acceso
     * @param actors de dónde sale el actor
     */
    public AuthorizationBehavior(AccessPolicy policy, ActorResolver actors) {
        this.policy = Objects.requireNonNull(policy, "policy");
        this.actors = Objects.requireNonNull(actors, "actors");
    }

    @Override
    public <R> R handle(Command<R> command, Next<R> next) {
        return authorize(command, next);
    }

    @Override
    public <R> R handle(Query<R> query, Next<R> next) {
        return authorize(query, next);
    }

    private <R> R authorize(Message<R> message, Next<R> next) {
        if (!policy.permits(actors.currentActor(), message)) {
            throw ApplicationError.forbidden(DENIED);
        }
        return next.proceed();
    }
}
