package io.quarkus.qute.debug.agent.evaluations;

import io.quarkus.qute.Engine;
import io.quarkus.qute.TemplateNode;

/**
 * Utility class to parse conditional expressions into Qute TemplateNodes.
 * <p>
 * In the debugger, breakpoints can have conditions (like `x > 5`).
 * This class allows converting such a string condition into a {@link TemplateNode}
 * that can be evaluated in the context of a template.
 * </p>
 */
public class ConditionalExpressionHelper {

    /**
     * A dedicated Qute engine used to parse conditions.
     * It's isolated from the user's template engine and preloaded with defaults.
     */
    private static final Engine conditionEngine;

    static {
        conditionEngine = Engine.builder().addDefaults().build();
    }

    /**
     * Parses a conditional expression string into a {@link TemplateNode}.
     * <p>
     * The method wraps the condition in a temporary if-else template:
     * {@code {#if <condition>}true{#else}false{/if}}
     * and returns the first node, which represents the conditional logic.
     * </p>
     *
     * @param condition the condition expression (e.g., "x > 5")
     * @return a TemplateNode representing the parsed conditional expression
     */
    public static TemplateNode parseCondition(String condition) {
        return conditionEngine
                .parse("{#if " + condition + "}true{#else}false{/if}")
                .findNodes(o -> true)
                .iterator()
                .next();
    }
}
