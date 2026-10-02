/**
 * El núcleo de CQRS de Nova, sin framework (ADR-053 y ADR-015).
 *
 * <p>Un {@link pe.edu.nova.java.libs.cqrs.Command} cambia el estado y una {@link pe.edu.nova.java.libs.cqrs.Query}
 * lo lee; los dos son records tipados por su resultado. El {@link pe.edu.nova.java.libs.cqrs.CommandBus} y el
 * {@link pe.edu.nova.java.libs.cqrs.QueryBus} encuentran el handler de cada uno por su clase exacta y lo ejecutan
 * dentro de una cadena de comportamientos, donde vive lo transversal.
 */
package pe.edu.nova.java.libs.cqrs;
