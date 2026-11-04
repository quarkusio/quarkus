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

public class CheckedTemplateParamRecordDuplicateNamesTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Hello.class)
                    .addAsResource(new StringAsset("Hello {name}!"),
                            "templates/CheckedTemplateParamRecordDuplicateNamesTest/Hello.txt"))
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
                assertTrue(te.getMessage().contains("Duplicate parameter name"),
                        te.getMessage());
            });

    @Test
    public void test() {
        fail("Should not be called - duplicate @CheckedTemplateParam names should cause build failure");
    }

    // This should fail because both components use the same name "name"
    record Hello(@CheckedTemplateParam("name") String firstName,
            @CheckedTemplateParam("name") String lastName) implements TemplateInstance {
    }

}
