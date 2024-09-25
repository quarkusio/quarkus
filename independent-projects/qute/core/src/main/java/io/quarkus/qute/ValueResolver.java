package io.quarkus.qute;

/**
 * Value resolvers are used when evaluating expressions.
 * <p>
 * First the resolvers that apply to the given {@link EvalContext} are filtered. Then the resolver with highest priority is used
 * to resolve the data. If a {@link io.quarkus.qute.Results.NotFound} object is returned then the next available resolver is
 * used instead. However,
 * {@code null} return value is considered a valid result.
 *
 * @see EvalContext
 * @see EngineBuilder#addValueResolver(ValueResolver)
 * @see EngineConfiguration
 */
public interface ValueResolver extends Resolver, WithPriority {

    /**
     * Value resolvers with higher priority take precedence.
     *
     * @return the priority value
     */
    @Override
    default int getPriority() {
        return WithPriority.super.getPriority();
    }

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