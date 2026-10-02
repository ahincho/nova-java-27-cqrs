package pe.edu.nova.java.libs.cqrs;

import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Qué handler ejecuta cada tipo de mensaje (ADR-053). Es inmutable y se arma con {@link #builder()}.
 *
 * <p>Las reglas que no cambian: un tipo de mensaje tiene exactamente un handler, el tipo es una clase concreta, y
 * un mensaje encuentra a su handler por su clase exacta, sin herencia, para que nunca haya dos candidatos.
 */
public final class HandlerRegistry {

    private final Map<Class<?>, CommandHandler<?, ?>> commands;
    private final Map<Class<?>, QueryHandler<?, ?>> queries;

    private HandlerRegistry(Builder builder) {
        this.commands = Map.copyOf(builder.commands);
        this.queries = Map.copyOf(builder.queries);
    }

    /**
     * Empieza un registro vacío.
     *
     * @return el builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * El handler del comando.
     *
     * @param command el comando
     * @param <R>     el tipo del resultado
     * @return su handler
     * @throws HandlerNotFoundException si ninguno ejecuta ese tipo de comando
     */
    @SuppressWarnings("unchecked") // el builder solo guarda un handler bajo el tipo de mensaje que declara
    public <R> CommandHandler<Command<R>, R> commandHandlerFor(Command<R> command) {
        CommandHandler<?, ?> handler = commands.get(command.getClass());
        if (handler == null) {
            throw new HandlerNotFoundException(command.getClass());
        }
        return (CommandHandler<Command<R>, R>) handler;
    }

    /**
     * El handler de la consulta.
     *
     * @param query la consulta
     * @param <R>   el tipo del resultado
     * @return su handler
     * @throws HandlerNotFoundException si ninguno responde ese tipo de consulta
     */
    @SuppressWarnings("unchecked") // el builder solo guarda un handler bajo el tipo de mensaje que declara
    public <R> QueryHandler<Query<R>, R> queryHandlerFor(Query<R> query) {
        QueryHandler<?, ?> handler = queries.get(query.getClass());
        if (handler == null) {
            throw new HandlerNotFoundException(query.getClass());
        }
        return (QueryHandler<Query<R>, R>) handler;
    }

    /**
     * Los tipos de comando que tienen handler.
     *
     * @return los tipos, inmutables
     */
    public Set<Class<?>> commandTypes() {
        return commands.keySet();
    }

    /**
     * Los tipos de consulta que tienen handler.
     *
     * @return los tipos, inmutables
     */
    public Set<Class<?>> queryTypes() {
        return queries.keySet();
    }

    /** Arma un {@link HandlerRegistry}. Cada registro se valida al hacerlo, no al ejecutar el mensaje. */
    public static final class Builder {

        private final Map<Class<?>, CommandHandler<?, ?>> commands = new LinkedHashMap<>();
        private final Map<Class<?>, QueryHandler<?, ?>> queries = new LinkedHashMap<>();

        private Builder() {}

        /**
         * Registra el handler de un tipo de comando, con el tipo escrito. Es la forma para una lambda.
         *
         * @param type    el tipo del comando
         * @param handler su handler
         * @param <C>     el tipo del comando
         * @param <R>     el tipo del resultado
         * @return este builder
         * @throws DuplicateHandlerException si el tipo ya tiene handler
         * @throws IllegalArgumentException  si el tipo no es una clase concreta
         */
        public <C extends Command<R>, R> Builder command(Class<C> type, CommandHandler<C, R> handler) {
            register(commands, type, handler);
            return this;
        }

        /**
         * Registra un handler de comando, leyendo de sus genéricos el tipo que ejecuta.
         *
         * @param handler el handler, una clase que implementa {@link CommandHandler} con sus genéricos
         * @return este builder
         * @throws DuplicateHandlerException si el tipo ya tiene handler
         * @throws IllegalArgumentException  si los genéricos no dicen el tipo, como en una lambda
         */
        public Builder command(CommandHandler<?, ?> handler) {
            register(commands, typeOf(handler, CommandHandler.class), handler);
            return this;
        }

        /**
         * Registra el handler de un tipo de consulta, con el tipo escrito. Es la forma para una lambda.
         *
         * @param type    el tipo de la consulta
         * @param handler su handler
         * @param <Q>     el tipo de la consulta
         * @param <R>     el tipo del resultado
         * @return este builder
         * @throws DuplicateHandlerException si el tipo ya tiene handler
         * @throws IllegalArgumentException  si el tipo no es una clase concreta
         */
        public <Q extends Query<R>, R> Builder query(Class<Q> type, QueryHandler<Q, R> handler) {
            register(queries, type, handler);
            return this;
        }

        /**
         * Registra un handler de consulta, leyendo de sus genéricos el tipo que responde.
         *
         * @param handler el handler, una clase que implementa {@link QueryHandler} con sus genéricos
         * @return este builder
         * @throws DuplicateHandlerException si el tipo ya tiene handler
         * @throws IllegalArgumentException  si los genéricos no dicen el tipo, como en una lambda
         */
        public Builder query(QueryHandler<?, ?> handler) {
            register(queries, typeOf(handler, QueryHandler.class), handler);
            return this;
        }

        /**
         * Arma el registro.
         *
         * @return el registro, inmutable
         */
        public HandlerRegistry build() {
            return new HandlerRegistry(this);
        }

        private static <H> void register(Map<Class<?>, H> handlers, Class<?> type, H handler) {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(handler, "handler");
            if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
                throw new IllegalArgumentException("A message type is a concrete class, such as a record, and "
                        + type.getName() + " is not: a message finds its handler by its exact class.");
            }
            H previous = handlers.putIfAbsent(type, handler);
            if (previous != null) {
                throw new DuplicateHandlerException(type, previous, handler);
            }
        }

        private static Class<?> typeOf(Object handler, Class<?> handlerInterface) {
            Objects.requireNonNull(handler, "handler");
            return HandlerTypes.messageTypeOf(handler.getClass(), handlerInterface)
                    .orElseThrow(() -> new IllegalArgumentException("The generics of "
                            + handler.getClass().getName() + " do not say which message it handles. Register it "
                            + "with its message type, as in command(PlaceOrder.class, handler)."));
        }
    }
}
