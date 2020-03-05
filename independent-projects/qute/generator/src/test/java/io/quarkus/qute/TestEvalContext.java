package io.quarkus.qute;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TestEvalContext implements EvalContext {

    private final Object base;
    private final String name;
    private final List<Expression> params;
    private Function<Expression, CompletionStage<Object>> evaluate;

    public TestEvalContext(Object base, String name,
            Function<Expression, CompletionStage<Object>> evaluate, String... params) {
        this.base = base;
        this.name = name;
        this.params = Arrays.stream(params).map(p -> Parser.parseExpression(p, Collections.emptyMap(), null))
                .collect(Collectors.toList());
        this.evaluate = evaluate;
    }

    @Override
    public Object getBase() {
        return base;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<Expression> getParams() {
        return params;
    }

    @Override
    public CompletionStage<Object> evaluate(String expression) {
        return evaluate(Parser.parseExpression(expression, Collections.emptyMap(), null));
    }

    @Override
    public CompletionStage<Object> evaluate(Expression expression) {
        return evaluate.apply(expression);
    }

}
