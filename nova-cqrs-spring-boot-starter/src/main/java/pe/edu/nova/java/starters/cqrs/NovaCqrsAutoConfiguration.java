package pe.edu.nova.java.starters.cqrs;

import io.micrometer.observation.ObservationRegistry;
import jakarta.validation.Validator;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ClassUtils;
import pe.edu.nova.java.libs.cqrs.ActorResolver;
import pe.edu.nova.java.libs.cqrs.CommandBehavior;
import pe.edu.nova.java.libs.cqrs.CommandBus;
import pe.edu.nova.java.libs.cqrs.HandlerRegistry;
import pe.edu.nova.java.libs.cqrs.QueryBehavior;
import pe.edu.nova.java.libs.cqrs.QueryBus;
import pe.edu.nova.java.libs.cqrs.SimpleCommandBus;
import pe.edu.nova.java.libs.cqrs.SimpleQueryBus;
import pe.edu.nova.java.libs.cqrs.access.AccessPolicy;
import pe.edu.nova.java.libs.cqrs.access.AuthorizationBehavior;
import pe.edu.nova.java.libs.cqrs.audit.AuditBehavior;
import pe.edu.nova.java.libs.cqrs.audit.AuditSink;

/**
 * Conecta el núcleo de CQRS con Spring Boot (ADR-053).
 *
 * <p>Registra el {@link CommandBus} y el {@link QueryBus} con los handlers que son beans del contexto, y los cinco
 * comportamientos de Nova en el orden de {@link NovaCqrsBehaviorOrder}. Cada pieza es un bean que el servicio
 * reemplaza declarando el suyo: los buses, el registro, el actor, la política de acceso y el destino de la
 * auditoría. Sin un handler, los buses no hacen nada.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "nova.cqrs", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(NovaCqrsProperties.class)
public class NovaCqrsAutoConfiguration {

    private static final String SPRING_SECURITY = "org.springframework.security.core.context.SecurityContextHolder";

    /** Crea la auto-configuración; la instancia Spring Boot. */
    public NovaCqrsAutoConfiguration() {}

    /**
     * Los handlers del contexto, cada uno bajo el tipo de mensaje que declara.
     *
     * @param beans la fábrica de beans
     * @return el registro
     */
    @Bean
    @ConditionalOnMissingBean
    public HandlerRegistry novaCqrsHandlerRegistry(ConfigurableListableBeanFactory beans) {
        return SpringHandlers.registry(beans);
    }

    /**
     * El bus de comandos, con los comportamientos en su orden.
     *
     * @param registry  los handlers
     * @param behaviors los comportamientos del contexto
     * @return el bus
     */
    @Bean
    @ConditionalOnMissingBean
    public CommandBus novaCommandBus(HandlerRegistry registry, ObjectProvider<CommandBehavior> behaviors) {
        return new SimpleCommandBus(registry, behaviors.orderedStream().toList());
    }

    /**
     * El bus de consultas, con los comportamientos en su orden.
     *
     * @param registry  los handlers
     * @param behaviors los comportamientos del contexto
     * @return el bus
     */
    @Bean
    @ConditionalOnMissingBean
    public QueryBus novaQueryBus(HandlerRegistry registry, ObjectProvider<QueryBehavior> behaviors) {
        return new SimpleQueryBus(registry, behaviors.orderedStream().toList());
    }

    /**
     * Quién ejecuta cada mensaje: el nombre de la autenticación si el servicio usa Spring Security, o nadie.
     *
     * @param context el contexto, para ver si está Spring Security
     * @return el resolvedor
     */
    @Bean
    @ConditionalOnMissingBean
    public ActorResolver novaCqrsActorResolver(ApplicationContext context) {
        if (ClassUtils.isPresent(SPRING_SECURITY, context.getClassLoader())) {
            return new SpringSecurityActorResolver();
        }
        return ActorResolver.anonymous();
    }

    /**
     * La política de acceso por defecto: permitir todo, para que el starter no niegue nada sin configurarlo.
     *
     * @return la política
     */
    @Bean
    @ConditionalOnMissingBean
    public AccessPolicy novaCqrsAccessPolicy() {
        return AccessPolicy.permitAll();
    }

    /**
     * El destino de la auditoría por defecto: el logger {@code nova.audit}.
     *
     * @return el destino
     */
    @Bean
    @ConditionalOnMissingBean
    public AuditSink novaCqrsAuditSink() {
        return new LoggingAuditSink();
    }

    /**
     * La auditoría de comandos y consultas.
     *
     * @param sink       el destino
     * @param actors     el actor
     * @param clocks     el reloj del servicio, si declara uno
     * @param properties la configuración
     * @return el comportamiento
     */
    @Bean
    @Order(NovaCqrsBehaviorOrder.AUDIT)
    @ConditionalOnProperty(prefix = "nova.cqrs.audit", name = "enabled", matchIfMissing = true)
    public AuditBehavior novaCqrsAuditBehavior(
            AuditSink sink, ActorResolver actors, ObjectProvider<Clock> clocks, NovaCqrsProperties properties) {
        return new AuditBehavior(
                sink,
                actors,
                clocks.getIfAvailable(Clock::systemUTC),
                properties.getAudit().isQueries());
    }

    /**
     * La autorización con la política de acceso.
     *
     * @param policy la política
     * @param actors el actor
     * @return el comportamiento
     */
    @Bean
    @Order(NovaCqrsBehaviorOrder.AUTHORIZATION)
    @ConditionalOnProperty(prefix = "nova.cqrs.authorization", name = "enabled", matchIfMissing = true)
    public AuthorizationBehavior novaCqrsAuthorizationBehavior(AccessPolicy policy, ActorResolver actors) {
        return new AuthorizationBehavior(policy, actors);
    }

    /** La observación, si el servicio tiene Micrometer. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ObservationRegistry.class)
    @ConditionalOnProperty(prefix = "nova.cqrs.observation", name = "enabled", matchIfMissing = true)
    static class ObservationConfiguration {

        @Bean
        @Order(NovaCqrsBehaviorOrder.OBSERVATION)
        ObservationBehavior novaCqrsObservationBehavior(ObjectProvider<ObservationRegistry> registries) {
            return new ObservationBehavior(registries);
        }
    }

    /** La validación, si el servicio tiene Bean Validation. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(Validator.class)
    @ConditionalOnProperty(prefix = "nova.cqrs.validation", name = "enabled", matchIfMissing = true)
    static class ValidationConfiguration {

        @Bean
        @Order(NovaCqrsBehaviorOrder.VALIDATION)
        ValidationBehavior novaCqrsValidationBehavior(ObjectProvider<Validator> validators) {
            return new ValidationBehavior(validators);
        }
    }

    /** La transacción, si el servicio tiene las transacciones de Spring. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(PlatformTransactionManager.class)
    @ConditionalOnProperty(prefix = "nova.cqrs.transaction", name = "enabled", matchIfMissing = true)
    static class TransactionConfiguration {

        @Bean
        @Order(NovaCqrsBehaviorOrder.TRANSACTION)
        TransactionBehavior novaCqrsTransactionBehavior(ObjectProvider<PlatformTransactionManager> managers) {
            return new TransactionBehavior(managers);
        }
    }
}
