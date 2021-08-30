package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateException;
import io.quarkus.test.QuarkusUnitTest;

public class InterfaceValidationFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(InterfaceValidationSuccessTest.Metrics.class, InterfaceValidationSuccessTest.Count.class,
                            InterfaceValidationSuccessTest.Wrapper.class)
                    .addAsResource(new StringAsset(
                            "{@io.quarkus.qute.deployment.typesafe.InterfaceValidationSuccessTest$Metrics metrics}"
                                    + "{metrics.responses.values}"),
                            "templates/metrics.html"))
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
                assertTrue(te.getMessage().contains("{metrics.responses.values}"), te.getMessage());
            });

    @Test
    public void test() {
        fail();
    }
}
