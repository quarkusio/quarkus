package io.quarkus.hibernate.reactive.runtime.customized;

import org.hibernate.reactive.context.impl.VertxContext;

import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;

/**
 * The {@link VertxContext} in Hibernate Reactive is accessing the Vert.x context directly, assuming this is the correct
 * context as intended by the developer, as Hibernate Reactive has no opinion in regard to how Vert.x is integrated with
 * other components. The precise definition of "correct context" will most likely depend on the runtime model and how
 * other components are integrated with Vert.x; in particular the lifecycle of the context needs to be specified. For
 * example in Quarkus's RestEasy Reactive we ensure that each request will run on a separate context; this ensures
 * operations relating to different web requests are isolated among each other. To ensure that Quarkus users are using
 * Hibernate Reactive on a context which is compatible with its expectations, this alternative implementation of
 * {@link VertxContext} actually checks on each context access if it's safe to use by invoking
 * {@link VertxContextSafetyToggle#validateContextIfExists(String, String)}.
 *
 * @see VertxContextSafetyToggle
 */
public final class CheckingVertxContext extends VertxContext {

    private static final String ERROR_MSG_ON_PROHIBITED_CONTEXT;
    private static final String ERROR_MSG_ON_UNKNOWN_CONTEXT;

    static {
        final String sharedmsg = " You can still use Hibernate Reactive, you just need to avoid using the methods which implicitly require accessing the stateful context, such as MutinySessionFactory#withTransaction and #withSession.";
        ERROR_MSG_ON_UNKNOWN_CONTEXT = "The current operation requires a safe (isolated) Vert.x sub-context, but the current context hasn't been flagged as such."
                + sharedmsg;
        ERROR_MSG_ON_PROHIBITED_CONTEXT = "The current Hibernate Reactive operation requires a safe (isolated) Vert.x sub-context, while the current context has been explicitly flagged as not compatible for this purpose."
                + sharedmsg;
    }

    @Override
    public <T> void put(Key<T> key, T instance) {
        VertxContextSafetyToggle.validateContextIfExists(ERROR_MSG_ON_PROHIBITED_CONTEXT, ERROR_MSG_ON_UNKNOWN_CONTEXT);
        super.put(key, instance);
    }

    @Override
    public <T> T get(Key<T> key) {
        VertxContextSafetyToggle.validateContextIfExists(ERROR_MSG_ON_PROHIBITED_CONTEXT, ERROR_MSG_ON_UNKNOWN_CONTEXT);
        return super.get(key);
    }

    @Override
    public void remove(Key<?> key) {
        VertxContextSafetyToggle.validateContextIfExists(ERROR_MSG_ON_PROHIBITED_CONTEXT, ERROR_MSG_ON_UNKNOWN_CONTEXT);
        super.remove(key);
    }

}
