package io.quarkus.arc.impl;

import io.quarkus.arc.InjectableReferenceProvider;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;

public final class InjectableReferenceProviders {

    private InjectableReferenceProviders() {
    }

    /**
     * Unwraps the provider if necessary and invokes {@link Contextual#destroy(Object, CreationalContext)}.
     *
     * @param <T>
     * @param provider
     * @param instance
     * @param creationalContext
     * @throws IllegalArgumentException If the specified provider is not a bean
     */
    @SuppressWarnings("unchecked")
    public static <T> void destroy(InjectableReferenceProvider<T> provider, T instance,
            CreationalContext<T> creationalContext) {
        if (provider instanceof CurrentInjectionPointProvider) {
            provider = ((CurrentInjectionPointProvider<T>) provider).getDelegate();
        }
        if (provider instanceof Contextual) {
            Contextual<T> contextual = (Contextual<T>) provider;
            contextual.destroy(instance, creationalContext);
        } else {
            throw new IllegalArgumentException("Injetable reference provider is not a bean: " + provider.getClass());
        }
    }

}
