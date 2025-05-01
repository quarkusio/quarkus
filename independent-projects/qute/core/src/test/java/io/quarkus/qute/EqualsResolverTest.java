package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

public class EqualsResolverTest {

    @Test
    public void tesEqualsResolver() {
        Engine engine = Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver()).build();
        assertEquals("false",
                engine.parse("{name eq 'Andy'}").data("name", "Martin").render());
        assertEquals("true",
                engine.parse("{name is 'Andy'}").data("name", "Andy").render());
        assertEquals("true",
                engine.parse("{name == 'Andy'}").data("name", "Andy").render());
        assertEquals("true",
                engine.parse("{negate.apply(name is 'Andy')}").data("name", "David").data("negate", new NegateFun()).render());
    }

    public static class NegateFun implements Function<Boolean, Boolean> {

        @Override
        public Boolean apply(Boolean value) {
            return !value;
        }

    }

}
