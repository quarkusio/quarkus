package io.quarkus.arc;

import io.quarkus.arc.InjectableContext.ContextState;
import java.lang.annotation.Annotation;

/**
 * This factory can be used to create a new {@link CurrentContext} for a normal scope, e.g. for
 * {@link jakarta.enterprise.context.RequestScoped}. It's usually not necessary for shared contexts, such as
 * {@link jakarta.enterprise.context.ApplicationScoped}.
 */
public interface CurrentContextFactory {

    <T extends ContextState> CurrentContext<T> create(Class<? extends Annotation> scope);

}
