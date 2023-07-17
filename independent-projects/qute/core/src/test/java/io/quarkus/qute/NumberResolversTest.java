package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class NumberResolversTest {

    @Test
    public void testSequence() {
        Engine engine = Engine.builder().addDefaults().build();
        int one = 1;
        assertEquals("0", engine.parse("{one plus 1 - 2}").data("one", one).render());
    }

    @Test
    public void testPlus() {
        Engine engine = Engine.builder().addDefaults().build();
        int one = 1;
        long intMaxTwoTimes = 4294967294l;
        int intMax = Integer.MAX_VALUE;

        assertEquals("2", engine.parse("{one plus 1}").data("one", one).render());
        assertEquals("2", engine.parse("{one.plus(1)}").data("one", one).render());
        assertEquals("256", engine.parse("{one + 255}").data("one", one).render());
        assertEquals("11", engine.parse("{one + 10l}").data("one", one).render());
        assertEquals("-2", engine.parse("{intMax plus intMax}").data("intMax", intMax).render());
        assertEquals("4294967295",
                engine.parse("{one + intMaxTwoTimes}").data("one", one, "intMaxTwoTimes", intMaxTwoTimes).render());
        assertEquals("4", engine.parse("{one plus 1 + 2}").data("one", one).render());
    }

    @Test
    public void testMinus() {
        Engine engine = Engine.builder().addDefaults().build();
        int one = 1;
        long intMaxTwoTimes = 4294967294l;
        int intMax = Integer.MAX_VALUE;

        assertEquals("0", engine.parse("{one minus 1}").data("one", one).render());
        assertEquals("-9", engine.parse("{one - 10}").data("one", one).render());
        assertEquals("-4", engine.parse("{one.minus(5)}").data("one", one).render());
        assertEquals(Integer.MAX_VALUE + "", engine.parse("{intMaxTwoTimes.minus(intMax)}")
                .data("intMaxTwoTimes", intMaxTwoTimes, "intMax", intMax).render());
    }

    @Test
    public void testMod() {
        Engine engine = Engine.builder().addDefaults().build();
        assertEquals("1", engine.parse("{eleven.mod(5)}").data("eleven", 11).render());
        assertEquals("1", engine.parse("{eleven mod 5}").data("eleven", 11).render());
    }

    @Test
    public void testNumberValue() {
        Engine engine = Engine.builder().addDefaults().build();
        int one = 1;
        int million = 1_000_000;
        double foo = 1.234d;

        assertEquals("1", engine.parse("{one.intValue}").data("one", one).render());
        assertEquals("1", engine.parse("{one.longValue}").data("one", one).render());
        assertEquals("1", engine.parse("{foo.intValue}").data("foo", foo).render());
        assertEquals("1.234", engine.parse("{foo.floatValue}").data("foo", foo).render());
        assertEquals("64", engine.parse("{million.byteValue}").data("million", million).render());
    }

}
