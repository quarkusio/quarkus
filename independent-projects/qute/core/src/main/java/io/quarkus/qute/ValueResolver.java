package io.quarkus.qute;

/**
 * Value resolvers are used when evaluating expressions.
 * <p>
 * First the resolvers that apply to the given {@link EvalContext} are filtered. Then the resolver with highest priority is used
 * to resolve the data. If {@link Results#isNotFound(Object)} is returned the next available resolver is tried.
 * 
 * @see EvalContext
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

    /**
     * 
     * @return a new builder
     */
    static ValueResolverBuilder builder() {
        return new ValueResolverBuilder();
    }

    // Utility methods

    static boolean matchClass(EvalContext ctx, Class<?> clazz) {
        return ctx.getBase() != null && clazz.isAssignableFrom(ctx.getBase().getClass());
    }

}