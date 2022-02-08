package io.quarkus.hibernate.reactive.runtime.customized;

import org.hibernate.reactive.context.impl.VertxContext;

import io.quarkus.hibernate.reactive.runtime.VertxContextSafetyToggle;

/**
 * The {@link VertxContext} in Hibernate Reactive is accessing the
 * Vert.x context directly, assuming this is the correct context as
 * intended by the developer, as Hibernate Reactive has no opinion in
 * regards to how Vert.x is integrated with other components.
 * The precise definition of "correct context" will most likely depend
 * on the runtime model and how other components are integrated with Vert.x;
 * in particular the lifecycle of the context needs to be specified.
 * For example in Quarkus's RestEasy Reactive we ensure that each request
 * will run on a separate context; this ensures operations relating to
 * different web requests are isolated among each other.
 * To ensure that Quarkus users are using Hibernate Reactive on a context
 * which is compatible with its expectations, this alternative implementation
 * of {@link VertxContext} actually checks on each context access if it's safe
 * to use by invoking {@link VertxContextSafetyToggle#assertContextUseAllowed()}.
 *
 * @see VertxContextSafetyToggle
 */
public final class CheckingVertxContext extends VertxContext {

    @Override
    public <T> void put(Key<T> key, T instance) {
        VertxContextSafetyToggle.assertContextUseAllowed().putLocal(key, instance);
    }

    @Override
    public <T> T get(Key<T> key) {
        return VertxContextSafetyToggle.assertContextUseAllowed().getLocal(key);
    }

    @Override
    public void remove(Key<?> key) {
        VertxContextSafetyToggle.assertContextUseAllowed().removeLocal(key);
    }

}
