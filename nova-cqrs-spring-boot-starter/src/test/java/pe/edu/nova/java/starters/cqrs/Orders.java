package pe.edu.nova.java.starters.cqrs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import pe.edu.nova.java.libs.cqrs.Command;
import pe.edu.nova.java.libs.cqrs.CommandHandler;
import pe.edu.nova.java.libs.cqrs.Query;
import pe.edu.nova.java.libs.cqrs.audit.AuditRecord;
import pe.edu.nova.java.libs.cqrs.audit.AuditSink;

/** Los mensajes, handlers y dobles de las pruebas del starter: un caso de pedidos, como el de ADR-053. */
final class Orders {

    private Orders() {}

    static final UUID PLACED = UUID.fromString("7f8f2a1c-6c1b-4e3a-9d2e-1b2c3d4e5f60");

    record PlaceOrder(@NotBlank String customerId, @Positive int quantity) implements Command<UUID> {}

    record FindOrder(UUID id) implements Query<String> {}

    static final class PlaceOrderHandler implements CommandHandler<PlaceOrder, UUID> {
        @Override
        public UUID handle(PlaceOrder command) {
            return PLACED;
        }
    }

    /** Un handler con {@code @Transactional}, que Spring envuelve en un proxy. */
    static class TransactionalPlaceOrderHandler implements CommandHandler<PlaceOrder, UUID> {
        @Override
        @Transactional
        public UUID handle(PlaceOrder command) {
            return PLACED;
        }
    }

    /** Un destino de auditoría que guarda los registros en memoria. */
    static final class RecordingAuditSink implements AuditSink {

        final List<AuditRecord> records = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void record(AuditRecord record) {
            records.add(record);
        }
    }

    /** Un gestor de transacciones que anota cada inicio, commit y rollback, sin base de datos. */
    static final class RecordingTransactionManager extends AbstractPlatformTransactionManager {

        private static final long serialVersionUID = 1L;

        final transient List<String> events = Collections.synchronizedList(new ArrayList<>());
        private final transient ThreadLocal<Boolean> active = ThreadLocal.withInitial(() -> false);

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected boolean isExistingTransaction(Object transaction) {
            return active.get();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            active.set(true);
            events.add(definition.isReadOnly() ? "begin:read-only" : "begin:read-write");
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            active.set(false);
            events.add("commit");
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            active.set(false);
            events.add("rollback");
        }

        @Override
        protected void doSetRollbackOnly(DefaultTransactionStatus status) {
            events.add("rollback-only");
        }
    }
}
