package io.quarkus.qute.deployment.loop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class LoopTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("{#for i in total}{i}:{/for}"), "templates/loop1.html"));

    @Inject
    Template loop1;

    @Test
    public void testIntegerIsIterable() {
        assertEquals("1:2:3:", loop1.data("total", 3).render());
    }

}
