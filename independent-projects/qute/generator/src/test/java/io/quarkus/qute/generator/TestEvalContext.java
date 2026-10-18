package io.quarkus.qute.generator;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.quarkus.qute.EvalContext;
import io.quarkus.qute.Expression;
import io.quarkus.qute.ResolutionContext;
import io.quarkus.qute.Scope;
import io.quarkus.qute.TemplateNode;

public class TestEvalContext implements EvalContext {

    private final Object base;
    private final String name;
    private final List<Expression> params;
    private Function<Expression, CompletionStage<Object>> evaluate;

    public TestEvalContext(Object base, String name,
            Function<Expression, CompletionStage<Object>> evaluate, String... params) {
        this.base = base;
        this.name = name;
        this.params = Arrays.stream(params).map(p -> Parser.parseExpression(() -> -1, p, Scope.EMPTY, null))
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
        return evaluate(Parser.parseExpression(() -> -1, expression, Scope.EMPTY, null));
    }

    @Override
    public CompletionStage<Object> evaluate(Expression expression) {
        return evaluate.apply(expression);
    }

    @Override
    public Object getAttribute(String key) {
        return null;
    }

    @Override
    public ResolutionContext resolutionContext() {
        return null;
    }

    private static final MethodHandle Parser_parseExpression;

    static class Parser {
        static Expression parseExpression(Supplier<Integer> idGenerator, String value, Scope scope,
                TemplateNode.Origin origin) {
            try {
                return (Expression) Parser_parseExpression.invokeExact(idGenerator, value, scope, origin);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable t) {
                throw new UndeclaredThrowableException(t);
            }
        }
    }

    static {
        try {
            Class<?> parser = Class.forName("io.quarkus.qute.Parser");
            Class<?> expressionImpl = Class.forName("io.quarkus.qute.ExpressionImpl");
            MethodHandles.Lookup privateLookup = MethodHandles.privateLookupIn(parser, MethodHandles.lookup());
            Parser_parseExpression = privateLookup.findStatic(parser, "parseExpression",
                    MethodType.methodType(expressionImpl, Supplier.class, String.class, Scope.class, TemplateNode.Origin.class))
                    .asType(MethodType.methodType(Expression.class, Supplier.class, String.class, Scope.class,
                            TemplateNode.Origin.class));
        } catch (Throwable t) {
            throw new IllegalStateException("Failed class init", t);
        }
    }
}
