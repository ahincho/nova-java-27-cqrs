/**
 * El conector de CQRS con Spring Boot (ADR-053).
 *
 * <p>Un servicio declara sus handlers como beans y usa el {@link pe.edu.nova.java.libs.cqrs.CommandBus} y el
 * {@link pe.edu.nova.java.libs.cqrs.QueryBus}. Cada mensaje pasa por la observación, la auditoría, la
 * autorización, la validación y la transacción, en ese orden, y cada una se apaga bajo {@code nova.cqrs.*}.
 */
package pe.edu.nova.java.starters.cqrs;
