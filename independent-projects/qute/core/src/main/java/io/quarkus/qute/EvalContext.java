package io.quarkus.qute;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * Evaluation context of a specific part of an {@link Expression}.
 * 
 * @see Expression#parts
 */
public interface EvalContext {

    /**
     * The base is always null for namespace resolvers.
     * 
     * @return the base object or null
     */
    Object getBase();

    /**
     * 
     * @return the name of the virtual property/function
     */
    String getName();

    /**
     * E.g. {@code name} and {@code age} if it represents a method invocation such as {@code foo(name,age)}. It is up to the
     * consumer to evaluate the params if needed.
     * 
     * @return the parameters
     */
    List<String> getParams();

    /**
     * Parse and evaluate the given expression using the relevant {@link ResolutionContext}
     * 
     * @param expression
     * @return the result
     */
    CompletionStage<Object> evaluate(String expression);

    /**
     * Evaluate the given expression using the relevant {@link ResolutionContext}.
     * 
     * @param expression
     * @return the result
     */
    CompletionStage<Object> evaluate(Expression expression);

}