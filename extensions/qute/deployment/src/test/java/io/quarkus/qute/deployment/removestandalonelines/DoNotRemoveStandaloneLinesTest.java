package io.quarkus.qute.deployment.removestandalonelines;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class DoNotRemoveStandaloneLinesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("{#for i in total}\n"
                            + "{i}:\n"
                            + "{/for}"), "templates/loop.html")
                    .addAsResource(new StringAsset("quarkus.qute.remove-standalone-lines=false"), "application.properties"));

    @Inject
    Template loop;

    @Test
    public void testLines() {
        assertEquals("\n1:\n\n2:\n\n3:\n", loop.data("total", 3).render());
    }

}
