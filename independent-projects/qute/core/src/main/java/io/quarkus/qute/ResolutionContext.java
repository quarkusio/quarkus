package io.quarkus.qute;

import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * The resolution context holds the current context object.
 */
public interface ResolutionContext {

    /**
     * Parse and evaluate the expression.
     *
     * @param expression
     * @return the result
     */
    CompletionStage<Object> evaluate(String expression);

    /**
     * Evaluate the expression.
     *
     * @param expression
     * @return the result
     */
    CompletionStage<Object> evaluate(Expression expression);

    /**
     * Create a child resolution context.
     *
     * @param data
     * @param extendingBlocks
     * @return a new child resolution context
     */
    ResolutionContext createChild(Object data, Map<String, SectionBlock> extendingBlocks);

    /**
     *
     * @return the data
     */
    Object getData();

    /**
     *
     * @return the parent context or null
     */
    ResolutionContext getParent();

    /**
     * If no extending block exists for the given name then the parent context (if present) is queried.
     *
     * @param name
     * @return the extending block for the specified name or {@code null}
     */
    SectionBlock getExtendingBlock(String name);

    /**
     * Unlike {@link #getExtendingBlock(String)} this method never queries the parent context.
     *
     * @param name
     * @return the extending block for the specified name or {@code null}
     */
    SectionBlock getCurrentExtendingBlock(String name);

    /**
     *
     * @param key
     * @return the attribute or null
     * @see TemplateInstance#getAttribute(String)
     */
    Object getAttribute(String key);

    /**
     * @return the current template
     */
    Template getTemplate();

    /**
     *
     * @return the evaluator
     */
    Evaluator getEvaluator();

}
