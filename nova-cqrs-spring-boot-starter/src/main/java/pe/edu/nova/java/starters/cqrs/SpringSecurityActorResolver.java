package pe.edu.nova.java.starters.cqrs;

import java.util.Optional;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import pe.edu.nova.java.libs.cqrs.ActorResolver;

/**
 * El actor de un servicio con Spring Security: el nombre de la autenticación en curso. Un usuario anónimo, o una
 * petición sin autenticación, no tiene actor.
 *
 * <p>Vive en su propia clase para que el starter no necesite Spring Security cuando el servicio no lo usa.
 */
final class SpringSecurityActorResolver implements ActorResolver {

    @Override
    public Optional<String> currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return Optional.ofNullable(authentication.getName()).filter(name -> !name.isBlank());
    }
}
