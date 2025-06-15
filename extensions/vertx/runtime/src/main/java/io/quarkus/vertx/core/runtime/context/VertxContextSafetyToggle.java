package io.quarkus.vertx.core.runtime.context;

import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * This is meant for other extensions to integrate with, to help identify which {@link Context}s are isolated and
 * guaranteeing access by a unique thread or event chain; for example it's used by Hibernate Reactive to check if the
 * current context is safe for it to store the currently opened session, to protect users from mistakenly interleaving
 * multiple reactive operations which could unintentionally share the same session. Vert.x web will create a new context
 * for each http web request; Quarkus RestEasy Reactive will mark such contexts as safe. Other extensions should follow
 * a similar pattern when they are setting up a new context which is safe to be used for the purposes of a local context
 * guaranteeing sequential use, non-concurrent access and scoped to the current reactive chain as a convenience to not
 * have to pass a "context" object along explicitly. In other cases it might be useful to explicitly mark the current
 * context as not safe instead; for example if an existing context needs to be shared across multiple workers to process
 * some operations in parallel: by marking and un-marking appropriately the same context can have spans in which it's
 * safe, followed by spans in which it's not safe. Normally we would expect the user to know and follow the caveats
 * expressed in the documentation of each project, but this additional facility helps to catch errors. The current
 * context can be explicitly marked as safe, or it can be explicitly marked as unsafe; there's a third state which is
 * the default of any new context: unmarked. The default is to consider any unmarked {@link Context} to be unsafe unless
 * the system property {@link #UNRESTRICTED_BY_DEFAULT_PROPERTY} is set to "true"; flip the default if you know what
 * you're doing and can't flag the context explicitly, but bear in mind that this will have a global impact and possibly
 * let other uses go unnoticed as well.
 */
public final class VertxContextSafetyToggle {

    private static final Object ACCESS_TOGGLE_KEY = new Object();
    public static final String UNRESTRICTED_BY_DEFAULT_PROPERTY = "io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle.UNRESTRICTED_BY_DEFAULT";

    /**
     * This gets exposed for people who prefer fully disabling all safeguards, for example because they have tested it
     * all carefully under load already and are preferring maximum efficiency over the safeguards introduced by this
     * class.
     */
    public static final String FULLY_DISABLE_PROPERTY = "io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle.I_HAVE_CHECKED_EVERYTHING";
    private static final boolean UNRESTRICTED_BY_DEFAULT = Boolean.getBoolean(UNRESTRICTED_BY_DEFAULT_PROPERTY);
    private static final boolean FULLY_DISABLED = Boolean.getBoolean(FULLY_DISABLE_PROPERTY);

    /**
     * Verifies if the current Vert.x context was flagged as safe to be accessed by components which expect
     * non-concurrent access to the current context, and its state isolated to the current stream. For example, it
     * checks if it's suitable to store a Session for Hibernate Reactive.
     *
     * @param errorMessageOnVeto
     *        the message to use for an {@link IllegalStateException}, should the context be explicitly marked as
     *        unsafe.
     * @param errorMessageOnDoubt
     *        the message to use for an {@link IllegalStateException}, in case the current context has no markers
     *        and flag RESTRICT_BY_DEFAULT has been set.
     *
     * @throws IllegalStateException
     *         if the context exists and it failed to be validated
     */
    public static void validateContextIfExists(final String errorMessageOnVeto, final String errorMessageOnDoubt) {
        if (FULLY_DISABLED)
            return;
        final io.vertx.core.Context context = Vertx.currentContext();
        if (context != null) {
            checkIsSafe(context, errorMessageOnVeto, errorMessageOnDoubt);
        }
    }

    private static void checkIsSafe(final Context context, final String errorMessageOnVeto,
            final String errorMessageOnDoubt) {
        if (!VertxContext.isDuplicatedContext(context)) {
            throw new IllegalStateException(
                    "Can't get the context safety flag: the current context is not a duplicated context");
        }
        final Object safeFlag = context.getLocal(ACCESS_TOGGLE_KEY);
        if (safeFlag == Boolean.TRUE) {
            return;
        } else if (safeFlag == null && UNRESTRICTED_BY_DEFAULT) {
            // flag was not set, and we're instructed to consider the undefined safeFlag as safe
            return;
        } else {
            if (safeFlag == null) {
                throw new IllegalStateException(errorMessageOnDoubt);
            } else {
                throw new IllegalStateException(errorMessageOnVeto);
            }
        }
    }

    /**
     * @param safe
     *        set to {@code true} to explicitly mark the current context as safe, or {@code false} to explicitly
     *        mark it as unsafe.
     *
     * @throws IllegalStateException
     *         if there is no current context, or if it's of the wrong type.
     */
    public static void setCurrentContextSafe(final boolean safe) {
        if (FULLY_DISABLED)
            return;
        final io.vertx.core.Context context = Vertx.currentContext();
        setContextSafe(context, safe);
    }

    /**
     * @param safe
     *        set to {@code true} to explicitly mark the current context as safe, or {@code false} to explicitly
     *        mark it as unsafe.
     * @param context
     *        The context to mark.
     *
     * @throws IllegalStateException
     *         if context is null or not of the expected type.
     */
    public static void setContextSafe(final Context context, final boolean safe) {
        if (FULLY_DISABLED)
            return;
        if (context == null) {
            throw new IllegalStateException("Can't set the context safety flag: no Vert.x context found");
        } else if (!VertxContext.isDuplicatedContext(context)) {
            throw new IllegalStateException(
                    "Can't set the context safety flag: the current context is not a duplicated context");
        } else {
            // save storm of true -> true transitions by shielding it
            if (safe && context.getLocal(ACCESS_TOGGLE_KEY) == Boolean.TRUE) {
                return;
            }
            context.putLocal(ACCESS_TOGGLE_KEY, Boolean.valueOf(safe));
        }
    }

    public static boolean isExplicitlyMarkedAsSafe(final Context context) {
        if (!VertxContext.isDuplicatedContext(context)) {
            throw new IllegalStateException(
                    "Can't get the context safety flag: the current context is not a duplicated context");
        }
        final Object safeFlag = context.getLocal(ACCESS_TOGGLE_KEY);
        if (safeFlag == Boolean.TRUE) {
            return true;
        }
        return false;
    }

    public static boolean isExplicitlyMarkedAsUnsafe(final Context context) {
        if (!VertxContext.isDuplicatedContext(context)) {
            throw new IllegalStateException(
                    "Can't get the context safety flag: the current context is not a duplicated context");
        }
        final Object safeFlag = context.getLocal(ACCESS_TOGGLE_KEY);
        if (safeFlag == Boolean.FALSE) {
            return true;
        }
        return false;
    }

}
