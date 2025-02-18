package io.quarkus.qute;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * Evaluation context of a specific part of an expression.
 *
 * @see Expression.Part
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
     * A virtual method may accept any number of parameters.
     * <p>
     * E.g. for a virtual method {@code foo(name.length,age)} the list of two expressions is returned, one for
     * {@code name.length} and the other one for {@code age}. It is up to the consumer to evaluate the params if needed.
     *
     * @return the list of parameters, is never {@code null}
     */
    List<Expression> getParams();

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

    /**
     *
     * @param key
     * @return the attribute or null
     * @see TemplateInstance#getAttribute(String)
     */
    Object getAttribute(String key);

    /**
     *
     * @return the current resolution context
     */
    ResolutionContext resolutionContext();

}