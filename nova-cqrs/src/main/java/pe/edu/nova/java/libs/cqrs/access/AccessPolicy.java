package pe.edu.nova.java.libs.cqrs.access;

import java.util.Optional;
import pe.edu.nova.java.libs.cqrs.Message;

/**
 * Si un actor puede ejecutar un mensaje: el puerto de la autorización (ADR-053).
 *
 * <p>Es el mecanismo general. Nova trae {@link #permitAll()}, para que el starter no niegue nada sin configurarlo;
 * los roles y permisos de Keycloak entran como otra implementación de este puerto, que lee los roles del token.
 */
@FunctionalInterface
public interface AccessPolicy {

    /**
     * Decide si el actor puede ejecutar el mensaje.
     *
     * @param actor   el actor, o vacío si no hay uno
     * @param message el mensaje, con su tipo y su contenido
     * @return {@code true} si puede
     */
    boolean permits(Optional<String> actor, Message<?> message);

    /**
     * Una política que permite todo.
     *
     * @return la política
     */
    static AccessPolicy permitAll() {
        return (actor, message) -> true;
    }
}
