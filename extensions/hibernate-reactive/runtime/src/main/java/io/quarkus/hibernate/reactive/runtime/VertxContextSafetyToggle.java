package io.quarkus.hibernate.reactive.runtime;

import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * This is meant for other extensions to integrate with, to help
 * identify in which contexts it's safe for Hibernate Reactive to
 * use the Vert.x {@link Context}.
 * The current context can be explicitly marked as safe, or it can
 * be explicitly marked as unsafe, or if no marker is found it will
 * by default be considered unsafe so to highlight possible integration
 * issues with other components.
 * The system property {@link #UNDEFINED_IS_SAFE_PROPERTY} can be set to
 * make the default "safe" instead; generally this should not be used:
 * if the current context is indeed to be considered as safe, the better
 * solution is to track down where it was created and ensure that this
 * specific context (and only this one) is marked as safe.
 * It might also be useful to explicitly mark certain contexts as unsafe,
 * as that will provide more useful error messages.
 */
public final class VertxContextSafetyToggle {

    private static final Object ACCESS_TOGGLE_KEY = new Object();
    public static final String UNDEFINED_IS_SAFE_PROPERTY = "io.quarkus.hibernate.reactive.runtime.VertxContextSafetyToggle.UNDEFINED_IS_SAFE";
    private static final boolean UNDEFINED_IS_SAFE = Boolean.getBoolean(UNDEFINED_IS_SAFE_PROPERTY);

    /**
     * Verifies if the current Vert.x context was flagged as valid
     * for the usage of Hibernate Reactive.
     * 
     * @return the validated Context instance
     * @throws IllegalStateException if there is no context, or if the context failed to be validated
     */
    public static Context assertContextUseAllowed() {
        final io.vertx.core.Context context = Vertx.currentContext();
        if (context == null) {
            throw new IllegalStateException(
                    "The current operation requires a Vert.x context to be active: none was found");
        } else {
            checkIsSafe(context);
            return context;
        }
    }

    private static void checkIsSafe(Context context) {
        final Object safeFlag = context.getLocal(ACCESS_TOGGLE_KEY);
        if (safeFlag == Boolean.TRUE) {
            return;
        } else if (safeFlag == null && UNDEFINED_IS_SAFE) {
            //flag was not set, and we're instructed to consider the undefined safeFlag as safe
            return;
        } else {
            final String sharedmsg = " You can still use Hibernate Reactive, you just need to avoid using the methods which implicitly require accessing the stateful context, such as MutinySessionFactory#withTransaction and #withSession.";
            if (safeFlag == null) {
                throw new IllegalStateException(
                        "The current operation requires a safe (isolated) Vert.x sub-context, but the current context hasn't been flagged as such. "
                                +
                                "This is most likely an integration problem; if you're sure this is a valid use case you can override by setting the "
                                +
                                "system property '" + UNDEFINED_IS_SAFE_PROPERTY
                                + "' to true. This will disable all similar checks, so it might be better to mark the current context specifically as safe. See "
                                + VertxContextSafetyToggle.class.getName() + "." + sharedmsg);
            } else {
                throw new IllegalStateException(
                        "The current operation requires a safe (isolated) Vert.x sub-context, while the current context has been explicitly flagged as not compatible for this purpose."
                                + sharedmsg);
            }
        }
    }

    /**
     * @param safe set to {@code true} to explicitly mark the current context as safe, or {@code false} to explicitly mark it as
     *        unsafe.
     */
    public static void setCurrentContextSafe(boolean safe) {
        final io.vertx.core.Context context = Vertx.currentContext();
        if (context == null) {
            throw new IllegalStateException("Can't set the context safety flag: no Vert.x context found");
        } else {
            context.putLocal(ACCESS_TOGGLE_KEY, Boolean.valueOf(safe));
        }
    }

}
