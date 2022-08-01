package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ValueResolverBuilderTest {

    @Test
    public void testBuilder() {
        Engine engine = Engine.builder().addDefaults()
                .addValueResolver(ValueResolver.builder()
                        .appliesTo(ec -> ec.getName().equals("foo"))
                        .resolveSync(ec -> "bar")
                        .build())
                .addValueResolver(ValueResolver.builder()
                        .applyToName("name")
                        .resolveWith("Spok")
                        .build())
                .addValueResolver(ValueResolver.builder()
                        .applyToName("age")
                        .resolveWith(1)
                        .priority(1)
                        .build())
                .addValueResolver(ValueResolver.builder()
                        .applyToName("age")
                        .resolveWith(10)
                        .priority(10)
                        .build())
                .addValueResolver(ValueResolver.builder()
                        .applyToBaseClass(String.class)
                        .applyToName("reverse")
                        .resolveSync(ec -> new StringBuilder(ec.getBase().toString()).reverse())
                        .build())
                .addValueResolver(ValueResolver.builder()
                        .applyToBaseClass(String.class)
                        .applyToName("upper")
                        .applyToNoParameters()
                        .resolveAsync(ec -> CompletedStage.of(ec.getBase().toString().toUpperCase()))
                        .build())
                .addValueResolver(ValueResolver.builder()
                        .applyToBaseClass(String.class)
                        .applyToName("upper")
                        .applyToParameters(1)
                        .resolveAsync(ec -> {
                            return ec.evaluate(ec.getParams().get(0)).thenApply(r -> {
                                return ec.getBase().toString().substring((int) r).toUpperCase();
                            });
                        })
                        .build())
                .build();

        assertEquals("bar", engine.parse("{foo}").render());
        assertEquals("Spok", engine.parse("{name}").render());
        assertEquals("10", engine.parse("{age}").render());
        assertEquals("zab", engine.parse("{val.reverse}").data("val", "baz").render());
        assertEquals("BAZ", engine.parse("{val.upper}").data("val", "baz").render());
        assertEquals("AZ", engine.parse("{val.upper(1)}").data("val", "baz").render());
    }

}
