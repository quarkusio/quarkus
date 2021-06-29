package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

public class StrictRenderingTest {

    @Test
    public void testStrictRendering() {
        Engine engine = Engine.builder().strictRendering(true).addDefaults().addValueResolver(new ReflectionValueResolver())
                .build();
        Hero hero = new Hero();
        hero.names = "John,Andy";
        try {
            engine.parse("{#if hero.nams}OK{/if}", null, "hero1").data("hero", hero).render();
            fail();
        } catch (TemplateException expected) {
            assertEquals(
                    "Property \"nams\" not found on the base object \"io.quarkus.qute.StrictRenderingTest$Hero\" in expression {hero.nams} in template hero1 on line 1",
                    expected.getMessage());
        }
        try {
            engine.parse("{hero.nams}", null, "hero2").data("hero", hero).render();
            fail();
        } catch (TemplateException expected) {
            assertEquals(
                    "Property \"nams\" not found on the base object \"io.quarkus.qute.StrictRenderingTest$Hero\" in expression {hero.nams} in template hero2 on line 1",
                    expected.getMessage());
        }
        assertEquals(
                "John,Andy",
                engine.parse("{hero.names}", null, "hero3").data("hero", hero).render());
    }

    public static class Hero {

        public String names;

    }

}
