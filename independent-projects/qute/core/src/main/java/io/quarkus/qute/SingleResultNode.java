package io.quarkus.qute;

import java.util.List;
import java.util.function.Consumer;

/**
 * A result node backed by an object value.
 */
public class SingleResultNode implements ResultNode {

    private final Object value;
    private final Expression expression;
    private final List<ResultMapper> mappers;

    public SingleResultNode(Object value, ExpressionNode expressionNode) {
        this.value = value;
        this.mappers = expressionNode.getEngine().getResultMappers().isEmpty() ? null
                : expressionNode.getEngine().getResultMappers();
        this.expression = this.mappers != null ? expressionNode.expression : null;
    }

    @Override
    public void process(Consumer<String> consumer) {
        if (value != null) {
            String result = null;
            if (mappers != null) {
                for (ResultMapper mapper : mappers) {
                    if (mapper.appliesTo(value)) {
                        result = mapper.map(value, expression);
                        break;
                    }
                }
            }
            if (result == null) {
                result = value.toString();
            }
            consumer.accept(result);
        }
    }

}
