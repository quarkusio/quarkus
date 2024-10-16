package io.quarkus.arc;

import java.lang.annotation.Annotation;

import io.quarkus.arc.InjectableContext.ContextState;

/**
 * This factory can be used to create a new {@link CurrentContext} for a normal scope.
 * <p>
 * For example, the current context for {@link jakarta.enterprise.context.RequestScoped}. It's usually not necessary for shared
 * contexts, such as {@link jakarta.enterprise.context.ApplicationScoped}.
 */
public interface CurrentContextFactory {

    /**
     *
     * @param <T>
     * @param scope
     * @return the current context
     * @throws IllegalStateException If the implementation does not support multiple current contexts for the same scope
     */
    <T extends ContextState> CurrentContext<T> create(Class<? extends Annotation> scope);

}
