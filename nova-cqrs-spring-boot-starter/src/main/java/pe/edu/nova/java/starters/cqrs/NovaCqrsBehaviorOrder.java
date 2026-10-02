package pe.edu.nova.java.starters.cqrs;

/**
 * El orden de los comportamientos de Nova, de afuera hacia adentro (ADR-053). El orden es parte del contrato.
 *
 * <p>Un comportamiento propio se declara como bean con {@code @Order} y queda entre los de Nova según su número:
 * con {@code @Order(350)} corre después de la autorización y antes de la validación. Los números dejan lugar entre
 * uno y otro para eso.
 */
public final class NovaCqrsBehaviorOrder {

    /** La observación de Micrometer: el timer y el span de cada mensaje. Va por fuera de todo. */
    public static final int OBSERVATION = 100;

    /** La auditoría, antes de la autorización para que un rechazo también quede registrado. */
    public static final int AUDIT = 200;

    /** La autorización, antes de validar: a quien no tiene permiso no se le dice qué campo está mal. */
    public static final int AUTHORIZATION = 300;

    /** La validación de Bean Validation sobre el mensaje. */
    public static final int VALIDATION = 400;

    /** La transacción, por dentro de todo, para que lo transversal no quede dentro del commit. */
    public static final int TRANSACTION = 500;

    private NovaCqrsBehaviorOrder() {}
}
