package io.quarkus.qute.debug.agent.evaluations;

import io.quarkus.qute.Engine;
import io.quarkus.qute.TemplateNode;

public class ConditionalExpressionHelper {

    private static final Engine conditionEngine;

    static {
        conditionEngine = Engine.builder().addDefaults().build();
    }

    public static TemplateNode parseCondition(String condition) {
        return conditionEngine.parse("{#if " + condition + "}true{#else}false{/if}")
                .findNodes(o -> true).iterator().next();
    }
}
