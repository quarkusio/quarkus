package io.quarkus.qute.deployment.propertynotfound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateException;
import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.test.QuarkusUnitTest;

public class PropertyNotFoundThrowExceptionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("foos:{foos}"), "templates/test.html")
                    .addAsResource(new StringAsset("quarkus.qute.property-not-found-strategy=throw-exception"
                            + "\nquarkus.qute.strict-rendering=false"),
                            "application.properties"));

    @Inject
    Template test;

    @Test
    public void testException() {
        try {
            test.render();
            fail();
        } catch (Exception expected) {
            Throwable rootCause = ExceptionUtil.getRootCause(expected);
            assertEquals(TemplateException.class, rootCause.getClass());
            assertTrue(rootCause.getMessage().contains("Entry \"foos\" not found in the data map"), rootCause.getMessage());
        }
    }

}
