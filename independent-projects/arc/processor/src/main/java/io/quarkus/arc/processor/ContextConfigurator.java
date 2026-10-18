package io.quarkus.arc.processor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.enterprise.context.NormalScope;

import io.quarkus.arc.ContextCreator;
import io.quarkus.arc.CurrentContextFactory;
import io.quarkus.arc.InjectableContext;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.Var;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.MethodDesc;

/**
 * Custom context configurator.
 *
 * @author Martin Kouba
 */
public final class ContextConfigurator {

    private final AtomicBoolean consumed;

    private final Consumer<ContextConfigurator> configuratorConsumer;

    Class<? extends Annotation> scopeAnnotation;

    boolean isNormal;

    Function<CreateGeneration, Expr> creator;

    final Map<String, Object> params;

    ContextConfigurator(Class<? extends Annotation> scopeAnnotation, Consumer<ContextConfigurator> configuratorConsumer) {
        this.consumed = new AtomicBoolean(false);
        this.scopeAnnotation = Objects.requireNonNull(scopeAnnotation);
        this.params = new HashMap<>();
        this.configuratorConsumer = configuratorConsumer;
        this.isNormal = scopeAnnotation.isAnnotationPresent(NormalScope.class);
    }

    public ContextConfigurator param(String name, Class<?> value) {
        params.put(name, value);
        return this;
    }

    public ContextConfigurator param(String name, int value) {
        params.put(name, value);
        return this;
    }

    public ContextConfigurator param(String name, long value) {
        params.put(name, value);
        return this;
    }

    public ContextConfigurator param(String name, double value) {
        params.put(name, value);
        return this;
    }

    public ContextConfigurator param(String name, String value) {
        params.put(name, value);
        return this;
    }

    public ContextConfigurator param(String name, boolean value) {
        params.put(name, value);
        return this;
    }

    /**
     * By default, the context is considered normal if the scope annotion is annotated with {@link NormalScope}.
     * <p>
     * It is possible to change this behavior. However, in such case the registrator is responsible for the correct
     * implementation of {@link InjectableContext#isNormal()}.
     *
     * @return self
     */
    public ContextConfigurator normal() {
        return normal(true);
    }

    /**
     * By default, the context is considered normal if the scope annotion is annotated with {@link NormalScope}.
     * <p>
     * It is possible to change this behavior. However, in such case the registrator is responsible for the correct
     * implementation of {@link InjectableContext#isNormal()}.
     *
     * @return self
     */
    public ContextConfigurator normal(boolean value) {
        this.isNormal = value;
        return this;
    }

    public ContextConfigurator contextClass(Class<? extends InjectableContext> contextClazz) {
        if (!Modifier.isPublic(contextClazz.getModifiers())
                || Modifier.isAbstract(contextClazz.getModifiers())
                || contextClazz.isAnonymousClass()
                || contextClazz.isLocalClass()
                || (contextClazz.getEnclosingClass() != null && !Modifier.isStatic(contextClazz.getModifiers()))) {
            throw new IllegalArgumentException(
                    "A context class must be a public non-abstract top-level or static nested class");
        }
        Constructor<?> constructor = getConstructor(contextClazz);
        if (constructor == null) {
            throw new IllegalArgumentException(
                    "A context class must either declare a no-args constructor or a constructor that accepts a single parameter of type io.quarkus.arc.CurrentContextFactory");
        }
        return creator(cg -> {
            BlockCreator bc = cg.method();

            List<Expr> args = constructor.getParameterCount() == 0 ? List.of() : List.of(cg.currentContextFactory());
            return bc.new_(ConstructorDesc.of(constructor), args);
        });
    }

    private Constructor<?> getConstructor(Class<? extends InjectableContext> contextClazz) {
        Constructor<?> constructor = null;
        try {
            constructor = contextClazz.getDeclaredConstructor(CurrentContextFactory.class);
        } catch (NoSuchMethodException ignored) {
        }
        if (constructor == null) {
            try {
                constructor = contextClazz.getDeclaredConstructor();
            } catch (NoSuchMethodException ignored) {
            }
        }
        return constructor;
    }

    public ContextConfigurator creator(Class<? extends ContextCreator> creatorClazz) {
        return creator(cg -> {
            BlockCreator bc = cg.method();

            LocalVar params = bc.localVar("params", bc.new_(HashMap.class));
            bc.withMap(params).put(Const.of(ContextCreator.KEY_CURRENT_CONTEXT_FACTORY), cg.currentContextFactory());
            for (Entry<String, Object> entry : this.params.entrySet()) {
                Expr value;
                if (entry.getValue() instanceof String s) {
                    value = Const.of(s);
                } else if (entry.getValue() instanceof Integer i) {
                    value = Const.of(i);
                } else if (entry.getValue() instanceof Long l) {
                    value = Const.of(l);
                } else if (entry.getValue() instanceof Double d) {
                    value = Const.of(d);
                } else if (entry.getValue() instanceof Class<?> c) {
                    value = Const.of(c);
                } else if (entry.getValue() instanceof Boolean b) {
                    value = Const.of(b);
                } else {
                    throw new IllegalArgumentException("Unknown parameter " + entry.getKey() + ": " + entry.getValue());
                }
                bc.withMap(params).put(Const.of(entry.getKey()), value);
            }
            Expr creator = bc.new_(creatorClazz);
            return bc.invokeInterface(
                    MethodDesc.of(ContextCreator.class, "create", InjectableContext.class, Map.class),
                    creator, params);
        });
    }

    public ContextConfigurator creator(Function<CreateGeneration, Expr> creator) {
        this.creator = creator;
        return this;
    }

    public void done() {
        if (consumed.compareAndSet(false, true)) {
            Objects.requireNonNull(creator);
            Objects.requireNonNull(configuratorConsumer).accept(this);
        }
    }

    public interface CreateGeneration {
        /**
         * {@return the {@link BlockCreator} for the method that instantiates the context object}
         * This method is supposed to contain the creation logic.
         */
        BlockCreator method();

        /**
         * {@return the variable that contains the {@link CurrentContextFactory}}
         */
        Var currentContextFactory();
    }
}
