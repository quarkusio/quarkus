package io.quarkus.qute;

import java.util.function.Consumer;

/**
 * A result node backed by a single object value.
 */
public class SingleResultNode implements ResultNode {

    private final Object value;
    private final ExpressionNode expressionNode;

    public SingleResultNode(Object value, ExpressionNode expressionNode) {
        this.value = value;
        this.expressionNode = expressionNode.getEngine().getResultMappers().isEmpty() ? null : expressionNode;
    }

    @Override
    public void process(Consumer<String> consumer) {
        if (value != null) {
            String result = null;
            if (expressionNode != null) {
                result = expressionNode.getEngine().mapResult(value, expressionNode.expression);
            } else {
                result = value.toString();
            }
            consumer.accept(result);
        }
    }

}
