package io.quarkus.qute;

/**
 * Value resolver.
 */
public interface ValueResolver extends Resolver, WithPriority {

    /**
     * 
     * @param context
     * @return {@code true} if this resolver applies to the given context
     */
    default boolean appliesTo(EvalContext context) {
        return true;
    }

    // Utility methods

    static boolean matchClass(EvalContext ctx, Class<?> clazz) {
        return ctx.getBase() != null && clazz.isAssignableFrom(ctx.getBase().getClass());
    }

}