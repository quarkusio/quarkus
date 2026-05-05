package io.quarkus.qute;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletionStage;

/**
 *
 * @see ValueResolver
 * @see NamespaceResolver
 */
public interface Resolver {

    /**
     * This method should return an instance of {@link Results#NotFound} if it's not possible to resolve the context. Any other
     * value is considered a valid result, including {@code null}.
     *
     * @param context
     * @return the result
     */
    CompletionStage<Object> resolve(EvalContext context);

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
}
