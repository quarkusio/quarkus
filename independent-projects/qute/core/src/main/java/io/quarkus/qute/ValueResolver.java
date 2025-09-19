package io.quarkus.qute;

import java.util.Collections;
import java.util.Set;

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
     * When {@link #appliesTo(EvalContext)} returns {@code true} for a specific {@link EvalContext} and the subsequent
     * invocation of {@link #resolve(EvalContext)} does not return {@link Results#NotFound} the value resolver returned from
     * this method is cached for the specific part of an expression.
     * <p>
     * By default, the resolver itself is cached. However, it is also possible to return an optimized version.
     *
     * @param context
     * @return the resolver that should be cached
     */
    default ValueResolver getCachedResolver(EvalContext context) {
        return this;
    }

    /**
     * Returns the set of property names supported by this value resolver for code completion in the Qute debugger.
     *
     * <p>
     * These properties are suggested when evaluating expressions on a base object. For example, if the user invokes
     * completion at {@code myList.|}, the evaluation context will be initialized with {@code myList} as the base object,
     * and {@link #appliesTo(io.quarkus.qute.EvalContext) appliesTo} will be called with that context. Only if it returns
     * {@code true} will the properties from this set be proposed.
     *
     * <p>
     * Completion examples:
     * <ul>
     * <li>{@code "length"} → inserts as-is: <code>myList.length|</code></li>
     * <li>{@code "size"} → inserts as-is: <code>myList.size|</code></li>
     * </ul>
     *
     * <p>
     * Example:
     *
     * <pre>{@code
     * @Override
     * public Set<String> getSupportedProperties() {
     *     return Set.of("length", "size");
     * }
     * }</pre>
     *
     * @return a set of supported property names to be shown in the debugger's code completion
     */
    default Set<String> getSupportedProperties() {
        return Collections.emptySet();
    }

    /**
     * Returns the set of method signatures supported by this value resolver for code completion in the Qute debugger.
     *
     * <p>
     * These methods are suggested when evaluating expressions on a base object. For example, if the user invokes
     * completion at {@code myList.|}, the evaluation context will be initialized with {@code myList} as the base object,
     * and {@link #appliesTo(io.quarkus.qute.EvalContext) appliesTo} will be called with that context. Only if it returns
     * {@code true} will the methods from this set be proposed.
     *
     * <p>
     * Completion examples:
     * <ul>
     * <li>{@code "take(index)"} → inserts as-is: <code>myList.take(index)|</code></li>
     * <li>{@code "takeLast(${index})"} → inserts with the parameter selected: <code>myList.takeLast(|[index])</code></li>
     * </ul>
     *
     * <p>
     * The {@code ${param}} syntax indicates that the debugger selects the parameter so the user can type it immediately.
     *
     * <p>
     * Example:
     *
     * <pre>{@code
     * @Override
     * public Set<String> getSupportedMethods() {
     *     return Set.of("take(index)", "takeLast(${index})");
     * }
     * }</pre>
     *
     * @return a set of supported method signatures to be shown in the debugger's code completion
     */
    default Set<String> getSupportedMethods() {
        return Collections.emptySet();
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
        return ValueResolvers.matchClass(ctx, clazz);
    }

}