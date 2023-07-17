package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkus.qute.TemplateLocator.TemplateLocation;
import io.quarkus.qute.TemplateNode.Origin;

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

    @Test
    public void testLocate() {
        assertEquals(Optional.empty(), Engine.builder().addDefaults().build().locate("foo"));
        Engine engine = Engine.builder().addDefaultSectionHelpers().addLocator(id -> Optional.of(new TemplateLocation() {

            @Override
            public Reader read() {
                return new StringReader("{foo}");
            }

            @Override
            public Optional<Variant> getVariant() {
                return Optional.empty();
            }

        })).build();
        Optional<TemplateLocation> location = engine.locate("foo");
        assertTrue(location.isPresent());
        try (Reader r = location.get().read()) {
            char[] buffer = new char[4096];
            StringBuilder b = new StringBuilder();
            int num;
            while ((num = r.read(buffer)) >= 0) {
                b.append(buffer, 0, num);
            }
            assertEquals("{foo}", b.toString());
        } catch (IOException e) {
            fail(e);
        }
    }

}
