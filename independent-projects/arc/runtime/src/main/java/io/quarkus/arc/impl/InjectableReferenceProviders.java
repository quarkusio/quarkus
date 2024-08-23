package io.quarkus.arc.impl;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;

import io.quarkus.arc.InjectableReferenceProvider;

public final class InjectableReferenceProviders {

    private InjectableReferenceProviders() {
    }

    /**
     * Unwraps the provider if necessary and invokes {@link Contextual#destroy(Object, CreationalContext)}.
     * <p>
     * If there is a parent context available then attempt to remove the dependent instance.
     *
     * @param <T>
     * @param provider
     * @param instance
     * @param creationalContext
     * @throws IllegalArgumentException If the specified provider is not a bean
     */
    public static <T> void destroy(InjectableReferenceProvider<T> provider, T instance,
            CreationalContext<T> creationalContext) {
        if (provider instanceof CurrentInjectionPointProvider) {
            provider = ((CurrentInjectionPointProvider<T>) provider).getDelegate();
        }
        if (provider instanceof Contextual) {
            @SuppressWarnings("unchecked")
            Contextual<T> contextual = (Contextual<T>) provider;
            contextual.destroy(instance, creationalContext);
            CreationalContextImpl<T> ctx = CreationalContextImpl.unwrap(creationalContext);
            CreationalContextImpl<?> parent = ctx.getParent();
            if (parent != null) {
                parent.removeDependentInstance(instance, false);
            }
        } else {
            throw new IllegalArgumentException("Injetable reference provider is not a bean: " + provider.getClass());
        }
    }

}
