package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

public class EvaluatedParamsTest {

    @Test
    public void testParameterTypesMatch() throws InterruptedException, ExecutionException {
        assertTrue(new EvaluatedParams(null, new Supplier[] { CompletedStage.of("Foo") })
                .parameterTypesMatch(false, new Class<?>[] { String.class }));
        assertTrue(new EvaluatedParams(null, new Supplier[] { CompletedStage.of(10) })
                .parameterTypesMatch(false, new Class<?>[] { Number.class }));
        assertTrue(new EvaluatedParams(null,
                new Supplier[] { CompletedStage.of(10), CompletedStage.of("Foo"),
                        CompletedStage.of("Bar") })
                .parameterTypesMatch(true, new Class<?>[] { Integer.class, Object[].class }));
        // varargs may be empty
        assertTrue(new EvaluatedParams(null,
                new Supplier[] { CompletedStage.of(10) })
                .parameterTypesMatch(true, new Class<?>[] { Integer.class, Object[].class }));
        assertFalse(new EvaluatedParams(null,
                new Supplier[] { CompletedStage.of("str") })
                .parameterTypesMatch(true, new Class[] { Locale.class, Object[].class }));

        // Integer,String does not match Integer,Object[]
        assertFalse(new EvaluatedParams(null,
                new Supplier[] { CompletedStage.of(10), CompletedStage.of("Foo") })
                .parameterTypesMatch(false, new Class<?>[] { Integer.class, Object[].class }));
    }

    @Test
    public void testInvalidParameter() {
        EvaluatedParams params = EvaluatedParams.evaluate(new EvalContext() {

            @Override
            public List<Expression> getParams() {
                Expression expr1 = ExpressionImpl.from("foo.bar");
                Expression expr2 = ExpressionImpl.from("bar.baz");
                return List.of(expr1, expr2);
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public Object getBase() {
                return null;
            }

            @Override
            public Object getAttribute(String key) {
                return null;
            }

            @Override
            public ResolutionContext resolutionContext() {
                return null;
            }

            @Override
            public CompletionStage<Object> evaluate(Expression expression) {
                if (expression.toOriginalString().equals("foo.bar")) {
                    return CompletedStage.of("foo");
                } else if (expression.toOriginalString().equals("bar.baz")) {
                    return CompletedStage.failure(new IllegalArgumentException());
                }
                throw new IllegalStateException();
            }

            @Override
            public CompletionStage<Object> evaluate(String expression) {
                return null;
            }
        });
        assertTrue(params.stage instanceof CompletedStage);
        CompletedStage<?> completed = (CompletedStage<?>) params.stage;
        assertTrue(completed.isFailure());
        try {
            completed.get();
            fail();
        } catch (TemplateException expected) {
            Throwable cause = expected.getCause();
            assertTrue(cause instanceof IllegalArgumentException);
        }
    }

}
