package io.quarkus.qute.deployment.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class NumberTemplateExtensionsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addAsResource(new StringAsset("{num.mod(4)}"),
                            "templates/foo.txt")
                    .addAsResource(
                            new StringAsset("{@java.lang.Integer val}{val.plus(1l).longValue}"),
                            "templates/intPlusLong.txt"));

    @Inject
    Template foo;

    @Inject
    Template intPlusLong;

    @Inject
    Engine engine;

    @Test
    public void testMod() {
        assertEquals("1", foo.data("num", 5).render());
        assertEquals("1", engine.parse("{eleven.mod(5)}").data("eleven", 11).render());
    }

    @Test
    public void testPlus() {
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

        assertEquals("2147483648", intPlusLong.data("val", Integer.MAX_VALUE).render());
    }

    @Test
    public void testMinus() {
        int one = 1;
        long intMaxTwoTimes = 4294967294l;
        int intMax = Integer.MAX_VALUE;

        assertEquals("0", engine.parse("{one minus 1}").data("one", one).render());
        assertEquals("-9", engine.parse("{one - 10}").data("one", one).render());
        assertEquals("-4", engine.parse("{one.minus(5)}").data("one", one).render());
        assertEquals(Integer.MAX_VALUE + "", engine.parse("{intMaxTwoTimes.minus(intMax)}")
                .data("intMaxTwoTimes", intMaxTwoTimes, "intMax", intMax).render());
        assertEquals("-2", engine.parse("{one minus 1 - 2}").data("one", one).render());

    }

}
