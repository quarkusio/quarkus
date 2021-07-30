package io.quarkus.qute;

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

}
