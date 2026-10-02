package pe.edu.nova.java.starters.cqrs;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import pe.edu.nova.java.libs.api.standard.error.ApplicationError;
import pe.edu.nova.java.libs.api.standard.error.FieldError;
import pe.edu.nova.java.libs.cqrs.Command;
import pe.edu.nova.java.libs.cqrs.CommandBehavior;
import pe.edu.nova.java.libs.cqrs.Message;
import pe.edu.nova.java.libs.cqrs.Next;
import pe.edu.nova.java.libs.cqrs.Query;
import pe.edu.nova.java.libs.cqrs.QueryBehavior;

/**
 * Valida cada mensaje con Bean Validation antes de abrir la transacción (ADR-053).
 *
 * <p>Si una restricción no se cumple, corta la cadena con un {@link ApplicationError} de tipo
 * {@code INVALID_INPUT}, un 400 con un error por campo, igual que un cuerpo que no pasa la validación en el
 * controlador. Una restricción sobre el mensaje entero lleva el campo vacío.
 */
public final class ValidationBehavior implements CommandBehavior, QueryBehavior {

    /** El mensaje para la persona, el mismo del catálogo de Nova para un 400. */
    static final String INVALID = "La solicitud no es válida";

    private final ObjectProvider<Validator> validators;

    /**
     * Crea el comportamiento. El validador se busca al ejecutar; sin uno, el mensaje pasa sin validar.
     *
     * @param validators el validador del servicio, si tiene uno
     */
    public ValidationBehavior(ObjectProvider<Validator> validators) {
        this.validators = Objects.requireNonNull(validators, "validators");
    }

    @Override
    public <R> R handle(Command<R> command, Next<R> next) {
        return validate(command, next);
    }

    @Override
    public <R> R handle(Query<R> query, Next<R> next) {
        return validate(query, next);
    }

    private <R> R validate(Message<R> message, Next<R> next) {
        Validator validator = validators.getIfAvailable();
        if (validator != null) {
            Set<ConstraintViolation<Message<R>>> violations = validator.validate(message);
            if (!violations.isEmpty()) {
                throw ApplicationError.invalidInput(INVALID, fieldErrors(violations));
            }
        }
        return next.proceed();
    }

    private static <T> List<FieldError> fieldErrors(Set<ConstraintViolation<T>> violations) {
        return violations.stream()
                .map(violation -> FieldError.of(violation.getPropertyPath().toString(), violation.getMessage()))
                .sorted(Comparator.comparing(FieldError::field).thenComparing(FieldError::message))
                .toList();
    }
}
