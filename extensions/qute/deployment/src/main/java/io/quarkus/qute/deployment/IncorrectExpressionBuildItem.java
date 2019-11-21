package io.quarkus.qute.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class IncorrectExpressionBuildItem extends MultiBuildItem {

    public final String expression;
    public final String property;
    public final String clazz;
    public final int line;
    public final String templateId;

    public IncorrectExpressionBuildItem(String expression, String property, String clazz, int line, String templateId) {
        this.expression = expression;
        this.property = property;
        this.clazz = clazz;
        this.line = line;
        this.templateId = templateId;
    }

}
