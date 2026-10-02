package pe.edu.nova.java.starters.cqrs;

import java.lang.reflect.Modifier;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.ResolvableType;
import pe.edu.nova.java.libs.cqrs.CommandHandler;
import pe.edu.nova.java.libs.cqrs.HandlerRegistry;
import pe.edu.nova.java.libs.cqrs.QueryHandler;

/**
 * Arma el {@link HandlerRegistry} con los handlers que son beans del contexto (ADR-053).
 *
 * <p>El tipo de mensaje de cada handler sale de su definición: la clase de un {@code @Component}, o el tipo que
 * devuelve un método {@code @Bean}, así que también funciona con una lambda declarada como
 * {@code CommandHandler<PlaceOrder, UUID>}. Si la definición no lo dice, se mira la clase real del bean, detrás de
 * un proxy como el de {@code @Transactional}. Dos handlers del mismo tipo impiden el arranque.
 */
final class SpringHandlers {

    private SpringHandlers() {}

    /**
     * El registro con todos los handlers del contexto.
     *
     * @param beans la fábrica de beans
     * @return el registro
     * @throws IllegalStateException si un handler no dice qué mensaje maneja, o si dos manejan el mismo
     */
    static HandlerRegistry registry(ConfigurableListableBeanFactory beans) {
        HandlerRegistry.Builder builder = HandlerRegistry.builder();
        for (String name : beans.getBeanNamesForType(CommandHandler.class)) {
            CommandHandler<?, ?> handler = beans.getBean(name, CommandHandler.class);
            registerCommand(builder, messageType(beans, name, handler, CommandHandler.class), handler);
        }
        for (String name : beans.getBeanNamesForType(QueryHandler.class)) {
            QueryHandler<?, ?> handler = beans.getBean(name, QueryHandler.class);
            registerQuery(builder, messageType(beans, name, handler, QueryHandler.class), handler);
        }
        return builder.build();
    }

    private static Class<?> messageType(
            ConfigurableListableBeanFactory beans, String name, Object handler, Class<?> handlerInterface) {
        Class<?> type = null;
        if (beans.containsBeanDefinition(name)) {
            BeanDefinition definition = beans.getMergedBeanDefinition(name);
            type = concrete(definition.getResolvableType().as(handlerInterface).resolveGeneric(0));
        }
        if (type == null) {
            Class<?> target = AopProxyUtils.ultimateTargetClass(handler);
            type = concrete(ResolvableType.forClass(target).as(handlerInterface).resolveGeneric(0));
        }
        if (type == null) {
            throw new IllegalStateException("The handler bean '" + name + "' does not say which message it "
                    + "handles. Declare its generics, as in CommandHandler<PlaceOrder, UUID>, on the class or on "
                    + "the return type of its @Bean method.");
        }
        return type;
    }

    /**
     * Un handler sin genéricos resuelve a la cota de su variable, como {@code Command}, y no a su mensaje: eso
     * cuenta como que la definición no lo dice.
     */
    private static Class<?> concrete(Class<?> type) {
        if (type == null || type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
            return null;
        }
        return type;
    }

    // El tipo salió de los genéricos del propio handler, así que el registro conserva el tipado del contrato
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerCommand(HandlerRegistry.Builder builder, Class<?> type, CommandHandler<?, ?> handler) {
        builder.command((Class) type, (CommandHandler) handler);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerQuery(HandlerRegistry.Builder builder, Class<?> type, QueryHandler<?, ?> handler) {
        builder.query((Class) type, (QueryHandler) handler);
    }
}
