package io.quarkus.qute;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.Consumer;

/**
 * A result node backed by a single object value.
 */
public final class SingleResultNode extends ResultNode {

    private final Object value;
    private final ExpressionNode node;

    public SingleResultNode(Object value) {
        this(value, null);
    }

    SingleResultNode(Object value, ExpressionNode expressionNode) {
        this.value = extractValue(value);
        this.node = expressionNode != null && expressionNode.hasEngineResultMappers() ? expressionNode : null;
    }

    private static Object extractValue(Object value) {
        if (value instanceof Optional) {
            return ((Optional<?>) value).orElse(null);
        }
        if (value instanceof OptionalInt) {
            return ((OptionalInt) value).orElse(0);
        }
        if (value instanceof OptionalDouble) {
            return ((OptionalDouble) value).orElse(0D);
        }
        if (value instanceof OptionalLong) {
            return ((OptionalLong) value).orElse(0L);
        }
        return value;
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
