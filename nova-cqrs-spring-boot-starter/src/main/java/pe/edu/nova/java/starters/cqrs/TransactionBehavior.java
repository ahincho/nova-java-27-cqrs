package pe.edu.nova.java.starters.cqrs;

import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.nova.java.libs.cqrs.Command;
import pe.edu.nova.java.libs.cqrs.CommandBehavior;
import pe.edu.nova.java.libs.cqrs.Next;
import pe.edu.nova.java.libs.cqrs.Query;
import pe.edu.nova.java.libs.cqrs.QueryBehavior;

/**
 * Corre cada mensaje en una transacción de Spring: de lectura y escritura en un comando, de solo lectura en una
 * consulta (ADR-053).
 *
 * <p>La propagación es {@code REQUIRED}, así que se suma a una transacción que ya exista. Es el caso de pedidos:
 * la idempotencia de ADR-047 abre la transacción alrededor del controlador, y el comando se confirma en ese mismo
 * commit. Una excepción del handler la deshace. Sin un gestor de transacciones, el mensaje corre sin ella.
 */
public final class TransactionBehavior implements CommandBehavior, QueryBehavior {

    private final ObjectProvider<PlatformTransactionManager> managers;
    private volatile Templates templates;

    /**
     * Crea el comportamiento. El gestor se busca en el primer mensaje, así que no depende del orden de la
     * configuración.
     *
     * @param managers el gestor de transacciones del servicio, si tiene uno
     */
    public TransactionBehavior(ObjectProvider<PlatformTransactionManager> managers) {
        this.managers = Objects.requireNonNull(managers, "managers");
    }

    @Override
    public <R> R handle(Command<R> command, Next<R> next) {
        Templates current = templates();
        return current == null ? next.proceed() : current.readWrite().execute(status -> next.proceed());
    }

    @Override
    public <R> R handle(Query<R> query, Next<R> next) {
        Templates current = templates();
        return current == null ? next.proceed() : current.readOnly().execute(status -> next.proceed());
    }

    private Templates templates() {
        Templates current = templates;
        if (current == null) {
            PlatformTransactionManager manager = managers.getIfAvailable();
            if (manager == null) {
                return null;
            }
            current = new Templates(template(manager, false), template(manager, true));
            templates = current;
        }
        return current;
    }

    private static TransactionTemplate template(PlatformTransactionManager manager, boolean readOnly) {
        TransactionTemplate template = new TransactionTemplate(manager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        template.setReadOnly(readOnly);
        return template;
    }

    private record Templates(TransactionTemplate readWrite, TransactionTemplate readOnly) {}
}
