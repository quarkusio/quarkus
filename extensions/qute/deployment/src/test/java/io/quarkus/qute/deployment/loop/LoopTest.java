package io.quarkus.qute.deployment.loop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusUnitTest;

public class LoopTest {

    static StringAsset template = new StringAsset("{#for i in total}{i}:{/for}");

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addAsResource(template, "templates/loop1.html")
                    .addAsResource(template, "templates/LoopTest/loopInt.html"));

    @CheckedTemplate
    static class Templates {

        static native TemplateInstance loopInt(int total);

    }

    @Inject
    Template loop1;

    @Test
    public void testIntegerIsIterable() {
        assertEquals("1:2:3:", loop1.data("total", 3).render());
        assertEquals("1:2:3:", Templates.loopInt(3).render());
    }

}
