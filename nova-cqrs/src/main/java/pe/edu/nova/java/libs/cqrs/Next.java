package pe.edu.nova.java.libs.cqrs;

/**
 * El resto de la cadena, visto desde un comportamiento: el siguiente comportamiento o, al final, el handler.
 *
 * @param <R> el tipo del resultado del mensaje
 */
@FunctionalInterface
public interface Next<R> {

    /**
     * Sigue con la cadena.
     *
     * @return lo que devolvió el resto de la cadena
     */
    R proceed();
}
