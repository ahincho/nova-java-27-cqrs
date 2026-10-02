package pe.edu.nova.java.libs.cqrs.audit;

/** Cómo terminó un mensaje auditado. */
public enum AuditOutcome {

    /** El handler devolvió su resultado. */
    SUCCEEDED,

    /** Se rechazó por falta de identidad o de permiso: un error {@code UNAUTHENTICATED} o {@code FORBIDDEN}. */
    DENIED,

    /** Terminó con cualquier otra excepción. */
    FAILED
}
