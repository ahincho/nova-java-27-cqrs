package pe.edu.nova.java.libs.cqrs;

import java.util.Optional;

/**
 * Quién ejecuta el mensaje en curso: el puerto que leen la auditoría y la autorización (ADR-053).
 *
 * <p>El starter de Spring Boot trae el nombre de la autenticación de Spring Security. Un servicio sin seguridad en
 * el propio proceso, como uno que recibe la identidad de un BFF en un header, declara el suyo.
 */
@FunctionalInterface
public interface ActorResolver {

    /**
     * El actor del mensaje en curso.
     *
     * @return su identificador, o vacío si no hay uno
     */
    Optional<String> currentActor();

    /**
     * Un resolvedor que nunca encuentra un actor.
     *
     * @return el resolvedor
     */
    static ActorResolver anonymous() {
        return Optional::empty;
    }
}
