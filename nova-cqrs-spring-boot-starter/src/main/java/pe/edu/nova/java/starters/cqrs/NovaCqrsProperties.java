package pe.edu.nova.java.starters.cqrs;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** La configuración de CQRS, bajo {@code nova.cqrs.*} (ADR-053). Todo está encendido por defecto. */
@ConfigurationProperties("nova.cqrs")
public class NovaCqrsProperties {

    /** Si el starter registra los buses. */
    private boolean enabled = true;

    /** La observación de Micrometer de cada mensaje. */
    private final Behavior observation = new Behavior();

    /** La auditoría de cada mensaje. */
    private final Audit audit = new Audit();

    /** La autorización de cada mensaje con la {@code AccessPolicy}. */
    private final Behavior authorization = new Behavior();

    /** La validación de Bean Validation de cada mensaje. */
    private final Behavior validation = new Behavior();

    /** La transacción de cada mensaje: de lectura y escritura en un comando, de solo lectura en una consulta. */
    private final Behavior transaction = new Behavior();

    /** Crea la configuración con los valores por defecto. */
    public NovaCqrsProperties() {}

    /**
     * Si el starter registra los buses.
     *
     * @return {@code true} por defecto
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enciende o apaga el starter entero.
     *
     * @param enabled si registra los buses
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * La observación.
     *
     * @return su configuración
     */
    public Behavior getObservation() {
        return observation;
    }

    /**
     * La auditoría.
     *
     * @return su configuración
     */
    public Audit getAudit() {
        return audit;
    }

    /**
     * La autorización.
     *
     * @return su configuración
     */
    public Behavior getAuthorization() {
        return authorization;
    }

    /**
     * La validación.
     *
     * @return su configuración
     */
    public Behavior getValidation() {
        return validation;
    }

    /**
     * La transacción.
     *
     * @return su configuración
     */
    public Behavior getTransaction() {
        return transaction;
    }

    /** Un comportamiento que se puede apagar. */
    public static class Behavior {

        /** Si el comportamiento está encendido. */
        private boolean enabled = true;

        /** Crea la configuración encendida. */
        public Behavior() {}

        /**
         * Si el comportamiento está encendido.
         *
         * @return {@code true} por defecto
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Enciende o apaga el comportamiento.
         *
         * @param enabled si está encendido
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /** La auditoría, que además decide si alcanza a las consultas. */
    public static class Audit extends Behavior {

        /** Si también se auditan las consultas. */
        private boolean queries = true;

        /** Crea la configuración encendida, con las consultas. */
        public Audit() {}

        /**
         * Si también se auditan las consultas.
         *
         * @return {@code true} por defecto
         */
        public boolean isQueries() {
            return queries;
        }

        /**
         * Deja dentro o fuera las consultas.
         *
         * @param queries si se auditan
         */
        public void setQueries(boolean queries) {
            this.queries = queries;
        }
    }
}
