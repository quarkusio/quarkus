package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.CheckedTemplateParam;
import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusExtensionTest;

public class CheckedTemplateParamRecordInvalidIdentifierTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Hello.class)
                    .addAsResource(new StringAsset("Hello {user.name}!"),
                            "templates/CheckedTemplateParamRecordInvalidIdentifierTest/Hello.txt"))
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
                assertTrue(te.getMessage().contains("@CheckedTemplateParam value must be a valid Java identifier"),
                        te.getMessage());
            });

    @Test
    public void test() {
        fail("Should not be called - invalid @CheckedTemplateParam value should cause build failure");
    }

    // This should fail because @CheckedTemplateParam has an invalid Java identifier (contains dot)
    record Hello(@CheckedTemplateParam("user.name") String userName) implements TemplateInstance {
    }

}
