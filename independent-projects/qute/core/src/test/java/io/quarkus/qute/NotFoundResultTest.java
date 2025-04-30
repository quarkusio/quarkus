package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.quarkus.qute.Results.NotFound;

public class NotFoundResultTest {

    @Test
    public void testAsMessage() {
        assertEquals("Key \"foo\" not found in the template data map with keys []",
                NotFound.from(evalContext(Map.of(TemplateInstanceBase.DATA_MAP_KEY, true))).asMessage());
        assertEquals("Key \"foo\" not found in the map with keys [baz]",
                NotFound.from(evalContext(Map.of("baz", true))).asMessage());
        assertEquals("Property \"foo\" not found on the base object \"java.lang.Boolean\"",
                NotFound.from(evalContext(Boolean.TRUE)).asMessage());
        assertEquals("Method \"foo(param)\" not found on the base object \"java.lang.Boolean\"",
                NotFound.from(evalContext(Boolean.TRUE, "param")).asMessage());
        assertEquals("Key \"foo\" not found in the map with keys [baz]",
                NotFound.from(evalContext(Mapper.wrap(Map.of("baz", false)))).asMessage());
    }

    EvalContext evalContext(Object base, Object... params) {
        return new EvalContext() {

            @Override
            public List<Expression> getParams() {
                return Arrays.stream(params).map(p -> ExpressionImpl.from(p.toString())).collect(Collectors.toList());
            }

            @Override
            public String getName() {
                return "foo";
            }

            @Override
            public Object getBase() {
                return base;
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
                return null;
            }

            @Override
            public CompletionStage<Object> evaluate(String expression) {
                return null;
            }
        };
    }

}
