package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateException;
import io.quarkus.test.QuarkusUnitTest;

public class WhenValidationFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Machine.class, MachineStatus.class)
                    .addAsResource(new StringAsset("{@io.quarkus.qute.deployment.typesafe.Machine machine}"
                            + "{#when machine.status}"
                            + "{#is WRONG} 1"
                            + "{#is item.name} 2"
                            + "{#is OFF} 0"
                            + "{/when}"), "templates/machine.html"))
            .assertException(t -> {
                Throwable e = t;
                TemplateException te = null;
                while (e != null) {
                    if (e instanceof TemplateException) {
                        te = (TemplateException) e;
                        break;
                    }
                    e = e.getCause();
                }
                assertNotNull(te);
                assertTrue(te.getMessage().contains("Found template problems (1)"), te.getMessage());
                assertTrue(te.getMessage().contains("{WRONG}"), te.getMessage());
            });

    @Test
    public void test() {
        fail();
    }

}
