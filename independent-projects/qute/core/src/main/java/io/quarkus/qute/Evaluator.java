package io.quarkus.qute;

import java.util.concurrent.CompletionStage;

/**
 * Evaluates expressions.
 */
public interface Evaluator {

    /**
     * @param expression
     * @param resolutionContext
     * @return the result
     */
    CompletionStage<Object> evaluate(Expression expression, ResolutionContext resolutionContext);

    /**
     * @return {@code true} if strict rendering is enforced, {@code false} otherwise
     */
    boolean strictRendering();

}
