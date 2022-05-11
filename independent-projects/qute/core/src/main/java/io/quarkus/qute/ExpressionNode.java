package io.quarkus.qute;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import org.jboss.logging.Logger;

/**
 * This node holds a single expression such as {@code foo.bar}.
 */
class ExpressionNode implements TemplateNode, Function<Object, CompletionStage<ResultNode>> {

    private static final Logger LOG = Logger.getLogger("io.quarkus.qute.nodeResolve");

    final ExpressionImpl expression;
    private final Engine engine;
    private final boolean traceLevel;

    ExpressionNode(ExpressionImpl expression, Engine engine) {
        this.expression = expression;
        this.engine = engine;
        this.traceLevel = LOG.isTraceEnabled();
    }

    @Override
    public CompletionStage<ResultNode> resolve(ResolutionContext context) {
        if (traceLevel) {
            LOG.tracef("Resolve {%s} started:%s", expression.toOriginalString(), expression.getOrigin());
        }
        return context.evaluate(expression).thenCompose(this);
    }

    @Override
    public CompletionStage<ResultNode> apply(Object result) {
        if (traceLevel) {
            LOG.tracef("Resolve {%s} completed:%s", expression.toOriginalString(), expression.getOrigin());
        }
        if (result instanceof ResultNode) {
            return CompletedStage.of((ResultNode) result);
        } else if (result instanceof CompletionStage) {
            return ((CompletionStage<?>) result).thenCompose(this);
        } else {
            return CompletedStage.of(new SingleResultNode(result, this));
        }
    }

    public Origin getOrigin() {
        return expression.getOrigin();
    }

    @Override
    public boolean isConstant() {
        return expression.isLiteral();
    }

    Engine getEngine() {
        return engine;
    }

    public List<Expression> getExpressions() {
        return Collections.singletonList(expression);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ExpressionNode [expression=").append(expression).append("]");
        return builder.toString();
    }

    boolean hasEngineResultMappers() {
        return !engine.getResultMappers().isEmpty();
    }

    String mapResult(Object result) {
        return engine.mapResult(result, expression);
    }

}
