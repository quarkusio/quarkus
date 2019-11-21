package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class SetSectionTest {

    @Test
    public void testSet() {
        Engine engine = Engine.builder().addDefaultSectionHelpers()
                .addDefaultValueResolvers()
                .build();
        assertEquals("NOT_FOUND - true:mix",
                engine.parse("{foo} - {#set foo=true bar='mix'}{foo}:{bar}{/}").instance().render());
    }

}
