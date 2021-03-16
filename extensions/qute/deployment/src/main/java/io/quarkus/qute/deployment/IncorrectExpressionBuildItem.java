package io.quarkus.qute.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.qute.TemplateNode.Origin;

public final class IncorrectExpressionBuildItem extends MultiBuildItem {

    public final String expression;
    public final String property;
    public final String clazz;
    public final Origin origin;
    public final String reason;

    public IncorrectExpressionBuildItem(String expression, String property, String clazz, Origin origin) {
        this(expression, property, clazz, origin, null);
    }

    public IncorrectExpressionBuildItem(String expression, String reason, Origin origin) {
        this(expression, null, null, origin, reason);
    }

    public IncorrectExpressionBuildItem(String expression, String property, String clazz, Origin origin,
            String reason) {
        this.expression = expression;
        this.property = property;
        this.clazz = clazz;
        this.origin = origin;
        this.reason = reason;
    }

}
