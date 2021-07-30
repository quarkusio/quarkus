package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.qute.TemplateNode.Origin;
import org.junit.jupiter.api.Test;

public class EngineTest {

    @Test
    public void testMapResut() {
        Engine engine = Engine.builder().addResultMapper((res, expr) -> "FOO").addResultMapper(new ResultMapper() {

            @Override
            public int getPriority() {
                // Is executed before the FOO mapper
                return 10;
            }

            @Override
            public boolean appliesTo(Origin origin, Object result) {
                return result instanceof Integer;
            }

            @Override
            public String map(Object result, Expression expression) {
                return "" + ((Integer) result) * 10;
            }
        }).build();
        Template test = engine.parse("{foo}");
        assertEquals("50",
                engine.mapResult(5, test.getExpressions().iterator().next()));
        assertEquals("FOO",
                engine.mapResult("bar", test.getExpressions().iterator().next()));
    }

}
