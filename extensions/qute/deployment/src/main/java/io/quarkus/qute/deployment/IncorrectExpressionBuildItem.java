package io.quarkus.qute.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class IncorrectExpressionBuildItem extends MultiBuildItem {

    public final String expression;
    public final String property;
    public final String clazz;
    public final int line;
    public final String templateId;
    public final String reason;

    public IncorrectExpressionBuildItem(String expression, String property, String clazz, int line, String templateId) {
        this(expression, property, clazz, line, templateId, null);
    }

    public IncorrectExpressionBuildItem(String expression, String reason, int line, String templateId) {
        this(expression, null, null, line, templateId, reason);
    }

    public IncorrectExpressionBuildItem(String expression, String property, String clazz, int line, String templateId,
            String reason) {
        this.expression = expression;
        this.property = property;
        this.clazz = clazz;
        this.line = line;
        this.templateId = templateId;
        this.reason = reason;
    }

}
