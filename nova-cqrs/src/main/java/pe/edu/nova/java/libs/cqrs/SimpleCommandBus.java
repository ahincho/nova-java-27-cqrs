package pe.edu.nova.java.libs.cqrs;

import java.util.List;
import java.util.Objects;

/**
 * El {@link CommandBus} de Nova: busca el handler en un {@link HandlerRegistry} y lo ejecuta dentro de la cadena
 * de comportamientos, en el orden en que llegan, el primero por fuera de todos.
 *
 * <p>El handler se busca antes de entrar en la cadena, así que un comando sin handler falla sin pasar por los
 * comportamientos.
 */
public final class SimpleCommandBus implements CommandBus {

    private final HandlerRegistry registry;
    private final List<CommandBehavior> behaviors;

    /**
     * Crea el bus.
     *
     * @param registry  los handlers
     * @param behaviors los comportamientos, de afuera hacia adentro
     */
    public SimpleCommandBus(HandlerRegistry registry, List<? extends CommandBehavior> behaviors) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.behaviors = List.copyOf(behaviors);
    }

    @Override
    public <R> R execute(Command<R> command) {
        Objects.requireNonNull(command, "command");
        CommandHandler<Command<R>, R> handler = registry.commandHandlerFor(command);
        return proceed(0, command, handler);
    }

    private <R> R proceed(int index, Command<R> command, CommandHandler<Command<R>, R> handler) {
        if (index == behaviors.size()) {
            return handler.handle(command);
        }
        return behaviors.get(index).handle(command, () -> proceed(index + 1, command, handler));
    }
}
