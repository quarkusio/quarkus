package io.quarkus.qute.deployment;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateData;
import io.quarkus.qute.TemplateException;
import io.quarkus.test.QuarkusUnitTest;

public class TemplateDataValidationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyClass.class)
                    .addAsResource(new StringAsset(
                            "{foo_My:BAZ}"),
                            "templates/foo.txt"))
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
                assertTrue(te.getMessage().contains("foo_My:BAZ"), te.getMessage());
            });

    @Test
    public void test() {
        fail();
    }

    @TemplateData(namespace = "foo_My")
    public static class MyClass {

        public static final String FOO = "foo";

    }

}
