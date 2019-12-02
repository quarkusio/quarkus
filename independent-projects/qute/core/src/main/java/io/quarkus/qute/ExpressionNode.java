package io.quarkus.qute;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * This node holds a single expression such as {@code foo.bar}.
 */
class ExpressionNode implements TemplateNode {

    final Expression expression;
    private final Engine engine;
    private final Origin origin;

    public ExpressionNode(Expression expression, Engine engine, Origin origin) {
        this.expression = expression;
        this.engine = engine;
        this.origin = origin;
    }

    @Override
    public CompletionStage<ResultNode> resolve(ResolutionContext context) {
        return context.evaluate(expression)
                .thenCompose(r -> CompletableFuture.<ResultNode> completedFuture(new SingleResultNode(r, this)));
    }

    public Origin getOrigin() {
        return origin;
    }

    Engine getEngine() {
        return engine;
    }

    public Set<Expression> getExpressions() {
        return Collections.singleton(expression);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ExpressionNode [expression=").append(expression).append("]");
        return builder.toString();
    }

}
