package io.quarkus.arc;

import java.lang.annotation.Annotation;

import io.quarkus.arc.InjectableContext.ContextState;

/**
 * This factory can be used to create a new {@link CurrentContext} for a normal scope, e.g. for
 * {@link javax.enterprise.context.RequestScoped}. It's usually not necessary for shared contexts, such as
 * {@link javax.enterprise.context.ApplicationScoped}.
 */
public interface CurrentContextFactory {

    <T extends ContextState> CurrentContext<T> create(Class<? extends Annotation> scope);

}
