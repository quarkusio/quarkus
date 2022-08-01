package io.quarkus.qute;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * A result node backed by a single object value.
 */
public final class SingleResultNode implements ResultNode {

    private final Object value;
    private final ExpressionNode node;

    public SingleResultNode(Object value, ExpressionNode expressionNode) {
        this.value = value instanceof Optional ? ((Optional<?>) value).orElse(null) : value;
        this.node = expressionNode.hasEngineResultMappers() ? expressionNode : null;
    }

    @Override
    public void process(Consumer<String> consumer) {
        if (value != null) {
            String result;
            if (node != null) {
                result = node.mapResult(value);
            } else {
                result = value.toString();
            }
            consumer.accept(result);
        }
    }

}
