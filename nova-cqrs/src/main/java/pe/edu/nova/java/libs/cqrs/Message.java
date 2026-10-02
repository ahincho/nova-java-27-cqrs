package pe.edu.nova.java.libs.cqrs;

/**
 * Lo que comparten un {@link Command} y una {@link Query}: un mensaje tipado por su resultado (ADR-053).
 *
 * <p>El parámetro {@code R} es lo que devuelve el bus al ejecutar el mensaje, así que el resultado sale del tipo
 * del mensaje y nunca hace falta un cast. Un servicio no implementa esta interfaz directamente: implementa
 * {@link Command} o {@link Query}, que son las que el bus sabe ejecutar.
 *
 * @param <R> el tipo del resultado
 */
public interface Message<R> {}
