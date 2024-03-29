package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusUnitTest;

public class CheckedTemplateArrayParamTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Templates.class, Color.class)
                    .addAsResource(new StringAsset("Hello {myArray[1]}! ::{myArray.take(1).size}"),
                            "templates/CheckedTemplateArrayParamTest/arrays.txt")
                    .addAsResource(new StringAsset("{array.0.asStr}"),
                            "templates/CheckedTemplateArrayParamTest/colors.txt"));

    @Test
    public void testBasePath() {
        assertEquals("Hello 1! ::1",
                Templates.arrays(new int[] { 0, 1 }).render());
        assertEquals("RED",
                Templates.colors(new Color[] { Color.RED, Color.BLUE }).render());
    }

    @CheckedTemplate
    public static class Templates {

        static native TemplateInstance arrays(int[] myArray);

        static native TemplateInstance colors(Color[] array);

    }

    public static enum Color {

        RED,
        BLUE;

        public String asStr() {
            return toString();
        }
    }

}
