package io.quarkus.arc.test.cdi.bcextensions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.NormalScope;
import jakarta.enterprise.context.spi.AlterableContext;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.Discovery;
import jakarta.enterprise.inject.build.compatible.spi.MetaAnnotations;
import jakarta.enterprise.inject.build.compatible.spi.Parameters;
import jakarta.enterprise.inject.build.compatible.spi.ScannedClasses;
import jakarta.enterprise.inject.build.compatible.spi.Synthesis;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticBeanCreator;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticComponents;
import jakarta.enterprise.inject.spi.BeanContainer;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;

// this test is basically a copy of https://github.com/weld/command-context-example
public class CustomNormalScopeTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(TestCommand.class, MyService.class, IdService.class)
            .buildCompatibleExtensions(new MyExtension())
            .build();

    @Test
    public void commandContextController() {
        CommandContextController control = Arc.container().select(CommandContextController.class).get();
        boolean activated = control.activate();
        assertTrue(activated);
        try {
            ArcContainer container = Arc.container();
            assertEquals(container.select(IdService.class).get().get(), container.select(IdService.class).get().get());
        } finally {
            control.deactivate();
        }
    }

    @Test
    public void commandExecutor() {
        CommandExecutor executor = Arc.container().select(CommandExecutor.class).get();
        executor.execute(() -> {
            ArcContainer container = Arc.container();
            assertEquals(container.select(IdService.class).get().get(), container.select(IdService.class).get().get());
        });
    }

    @Test
    public void commandDecorator() {
        TestCommand command = Arc.container().select(TestCommand.class).get();
        command.execute(); // contains assertions
        assertTrue(TestCommand.EXECUTED);
    }

    @Dependent
    static class TestCommand implements Command {
        static boolean EXECUTED = false;

        @Inject
        CommandExecution execution;

        @Inject
        MyService service;

        @Inject
        IdService id;

        @Override
        public void execute() {
            service.process();
            assertEquals(id.get(), execution.getData().get("id"));
            assertNotNull(execution.getStartedAt());
            EXECUTED = true;
        }
    }

    @ApplicationScoped
    static class MyService {
        @Inject
        CommandExecution execution;

        @Inject
        IdService id;

        void process() {
            execution.getData().put("id", id.get());
        }
    }

    @CommandScoped
    static class IdService {
        private final String id = UUID.randomUUID().toString();

        public String get() {
            return id;
        }
    }

    // ---

    public static class MyExtension implements BuildCompatibleExtension {
        @Discovery
        public void discovery(MetaAnnotations meta, ScannedClasses scan) {
            meta.addContext(CommandScoped.class, CommandContext.class);
            scan.add(CommandExecutor.class.getName());
            scan.add(CommandDecorator.class.getName());
        }

        @Synthesis
        public void synthesis(SyntheticComponents syn) {
            syn.addBean(CommandContextController.class)
                    .type(CommandContextController.class)
                    .scope(Dependent.class)
                    .createWith(CommandContextControllerCreator.class);

            syn.addBean(CommandExecution.class)
                    .type(CommandExecution.class)
                    .scope(CommandScoped.class)
                    .createWith(CommandExecutionCreator.class);
        }
    }

    static class CommandContextControllerCreator implements SyntheticBeanCreator<CommandContextController> {
        @Override
        public CommandContextController create(Instance<Object> lookup, Parameters params) {
            BeanContainer beanContainer = lookup.select(BeanContainer.class).get();
            CommandContext ctx = (CommandContext) beanContainer.getContexts(CommandScoped.class).iterator().next();
            return new CommandContextController(ctx, beanContainer);
        }
    }

    static class CommandExecutionCreator implements SyntheticBeanCreator<CommandExecution> {
        @Override
        public CommandExecution create(Instance<Object> lookup, Parameters params) {
            CommandContext ctx = (CommandContext) lookup.select(BeanContainer.class).get().getContext(CommandScoped.class);
            return ctx.getCurrentCommandExecution();
        }
    }

    // ---

    /**
     * A <em>command</em>. Commands may be beans, but don't necessarily have to.
     * For commands that are beans, the {@linkplain CommandScoped command context}
     * is automatically activated by the {@link CommandDecorator CommandDecorator}.
     * For commands that are not beans, the {@link CommandExecutor CommandExecutor}
     * should be used to activate/deactivate the command context.
     */
    @FunctionalInterface
    public interface Command {
        void execute();
    }

    /**
     * Specifies that a bean belongs to the <em>command</em> normal scope.
     * <p>
     * A dependent-scoped bean of type {@link CommandContextController CommandContextController}
     * is provided that may be used to manually activate and deactivate the command context.
     * <p>
     * A dependent-scoped bean of type {@link CommandExecutor CommandExecutor} is provided that
     * may be used to execute a {@link Command Command} implementation which is not a bean.
     * <p>
     * All beans that implement {@link Command Command} are decorated by the {@link CommandDecorator CommandDecorator},
     * which automatically activates (and deactivates) the command context for the duration of the
     * {@link Command#execute() Command.execute()} method.
     * <p>
     * A command-scoped bean of type {@link CommandExecution CommandExecution} is provided that contains
     * certain details about the command execution and allows exchanging data between beans in the same command scope.
     */
    @NormalScope
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    public @interface CommandScoped {
    }

    static class CommandExecution {
        private final Date startedAt;

        private final Map<String, Object> data;

        CommandExecution() {
            this.startedAt = new Date();
            this.data = new HashMap<>();
        }

        Date getStartedAt() {
            return startedAt;
        }

        Map<String, Object> getData() {
            return data;
        }
    }

    /**
     * Allows manual activation and deactivation of the {@linkplain CommandScoped command} context.
     * The {@code activate()} method returns {@code true} if the command context was not
     * active on the current thread at the moment of the call and hence was activated by the call.
     * When the command context was active on the current thread when {@code activate()} is called,
     * {@code false} is returned and the operation is otherwise a noop.
     * <p>
     * When {@code activate()} returns {@code true}, the caller is supposed to call
     * {@code deactivate()} later on. Calling {@code deactivate()} when the command context
     * is not active leads to {@code ContextNotActiveException}. Calling {@code deactivate()}
     * when the command context is active but was not activated by this controller is a noop.
     */
    static final class CommandContextController {
        private final CommandContext context;

        private final BeanContainer beanContainer;

        private final AtomicBoolean activated = new AtomicBoolean(false);

        CommandContextController(CommandContext context, BeanContainer beanContainer) {
            this.context = context;
            this.beanContainer = beanContainer;
        }

        public boolean activate() {
            try {
                beanContainer.getContext(CommandScoped.class);
                return false;
            } catch (ContextNotActiveException e) {
                context.activate();
                activated.set(true);
                return true;
            }
        }

        public void deactivate() throws ContextNotActiveException {
            beanContainer.getContext(CommandScoped.class);
            if (activated.compareAndSet(true, false)) {
                context.deactivate();
            }
        }
    }

    /**
     * Executes a {@link Command Command} implementation which is not a bean in the command scope.
     * That is, the command context is activated before calling {@code Command.execute()}
     * and deactivated when {@code Command.execute()} returns (or throws).
     * <p>
     * {@code CommandExecutor} should not be used with {@code Command}s that are beans. Their
     * {@code execute()} invocation is automatically decorated by {@link CommandDecorator CommandDecorator},
     * so context activation and deactivation is handled automatically.
     */
    @Dependent
    static class CommandExecutor {
        private final CommandContextController control;

        @Inject
        CommandExecutor(CommandContextController control) {
            this.control = control;
        }

        public void execute(Command command) {
            try {
                control.activate();
                command.execute();
            } finally {
                control.deactivate();
            }
        }
    }

    /**
     * Decorates all {@code Command}s that are beans and automatically activates
     * (and deactivates) the {@linkplain CommandScoped command} context for the duration
     * of the {@code execute()} invocation.
     */
    @Decorator
    @Priority(Interceptor.Priority.LIBRARY_BEFORE)
    static abstract class CommandDecorator implements Command {
        @Inject
        @Delegate
        Command delegate;

        private CommandContextController control;

        @Inject
        CommandDecorator(CommandContextController control) {
            this.control = control;
        }

        @Override
        public void execute() {
            try {
                control.activate();
                delegate.execute();
            } finally {
                control.deactivate();
            }
        }
    }

    public static class CommandContext implements AlterableContext {
        private final ThreadLocal<Map<Contextual<?>, ContextualInstance<?>>> currentContext = new ThreadLocal<>();
        private final ThreadLocal<CommandExecution> currentCommandExecution = new ThreadLocal<>();

        public Class<? extends Annotation> getScope() {
            return CommandScoped.class;
        }

        public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
            Map<Contextual<?>, ContextualInstance<?>> store = currentContext.get();

            if (store == null) {
                throw new ContextNotActiveException();
            }

            ContextualInstance<T> instance = (ContextualInstance<T>) store.get(contextual);
            if (instance == null && creationalContext != null) {
                instance = new ContextualInstance<T>(contextual.create(creationalContext), creationalContext, contextual);
                store.put(contextual, instance);
            }
            return instance != null ? instance.get() : null;
        }

        public <T> T get(Contextual<T> contextual) {
            return get(contextual, null);
        }

        public boolean isActive() {
            return currentContext.get() != null;
        }

        public void destroy(Contextual<?> contextual) {
            Map<Contextual<?>, ContextualInstance<?>> ctx = currentContext.get();
            if (ctx == null) {
                return;
            }
            ContextualInstance<?> contextualInstance = ctx.remove(contextual);
            if (contextualInstance != null) {
                contextualInstance.destroy();
            }
        }

        void activate() {
            currentContext.set(new HashMap<>());
            currentCommandExecution.set(new CommandExecution());
        }

        void deactivate() {
            Map<Contextual<?>, ContextualInstance<?>> ctx = currentContext.get();
            if (ctx == null) {
                return;
            }
            for (ContextualInstance<?> instance : ctx.values()) {
                try {
                    instance.destroy();
                } catch (Exception e) {
                    System.err.println("Unable to destroy instance" + instance.get() + " for bean: "
                            + instance.getContextual());
                }
            }
            ctx.clear();
            currentContext.remove();
            currentCommandExecution.remove();
        }

        CommandExecution getCurrentCommandExecution() {
            return currentCommandExecution.get();
        }

        static final class ContextualInstance<T> {
            private final T value;
            private final CreationalContext<T> creationalContext;
            private final Contextual<T> contextual;

            ContextualInstance(T instance, CreationalContext<T> creationalContext, Contextual<T> contextual) {
                this.value = instance;
                this.creationalContext = creationalContext;
                this.contextual = contextual;
            }

            T get() {
                return value;
            }

            Contextual<T> getContextual() {
                return contextual;
            }

            void destroy() {
                contextual.destroy(value, creationalContext);
            }
        }
    }
}
