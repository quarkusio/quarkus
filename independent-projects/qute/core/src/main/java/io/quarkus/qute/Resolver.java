package io.quarkus.qute;

import io.quarkus.qute.Results.Result;
import java.util.concurrent.CompletionStage;

/**
 * 
 * @see ValueResolver
 * @see NamespaceResolver
 */
public interface Resolver {

    /**
     * This method should return {@link Result#NOT_FOUND} if it's not possible to resolve the context. Any other value is
     * considered a valid result, including {@code null}.
     * 
     * @param context
     * @return the result
     */
    CompletionStage<Object> resolve(EvalContext context);

}
