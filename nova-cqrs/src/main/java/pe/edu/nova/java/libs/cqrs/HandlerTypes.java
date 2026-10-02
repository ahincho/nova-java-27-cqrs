package pe.edu.nova.java.libs.cqrs;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Encuentra el tipo de mensaje que declara un handler leyendo sus genéricos, para registrarlo sin repetir la clase.
 *
 * <p>Recorre las interfaces y las superclases de la clase del handler y sigue las variables de tipo, así que
 * también encuentra el tipo cuando el handler hereda de una clase abstracta genérica. No lo encuentra en una
 * lambda ni en una clase que implementa la interfaz sin genéricos: ahí no queda escrito en ningún lado.
 */
final class HandlerTypes {

    private HandlerTypes() {}

    /**
     * El primer argumento de tipo con el que {@code handlerClass} implementa {@code handlerInterface}.
     *
     * @param handlerClass     la clase del handler
     * @param handlerInterface {@link CommandHandler} o {@link QueryHandler}
     * @return el tipo del mensaje, o vacío si los genéricos no lo dicen
     */
    static Optional<Class<?>> messageTypeOf(Class<?> handlerClass, Class<?> handlerInterface) {
        return resolve(handlerClass, handlerInterface, Map.of());
    }

    private static Optional<Class<?>> resolve(
            Type type, Class<?> handlerInterface, Map<TypeVariable<?>, Type> bindings) {
        Class<?> raw;
        Map<TypeVariable<?>, Type> current = new HashMap<>();
        if (type instanceof ParameterizedType parameterized) {
            raw = (Class<?>) parameterized.getRawType();
            TypeVariable<?>[] variables = raw.getTypeParameters();
            Type[] arguments = parameterized.getActualTypeArguments();
            for (int i = 0; i < variables.length; i++) {
                current.put(variables[i], substitute(arguments[i], bindings));
            }
        } else if (type instanceof Class<?> plain) {
            raw = plain;
        } else {
            return Optional.empty();
        }
        if (raw.equals(handlerInterface)) {
            return toClass(current.get(handlerInterface.getTypeParameters()[0]));
        }
        for (Type parent : raw.getGenericInterfaces()) {
            Optional<Class<?>> found = resolve(parent, handlerInterface, current);
            if (found.isPresent()) {
                return found;
            }
        }
        Type superclass = raw.getGenericSuperclass();
        return superclass == null ? Optional.empty() : resolve(superclass, handlerInterface, current);
    }

    private static Type substitute(Type argument, Map<TypeVariable<?>, Type> bindings) {
        if (argument instanceof TypeVariable<?> variable) {
            return bindings.getOrDefault(variable, variable);
        }
        return argument;
    }

    private static Optional<Class<?>> toClass(Type type) {
        if (type instanceof Class<?> plain) {
            return Optional.of(plain);
        }
        if (type instanceof ParameterizedType parameterized) {
            return Optional.of((Class<?>) parameterized.getRawType());
        }
        return Optional.empty();
    }
}
