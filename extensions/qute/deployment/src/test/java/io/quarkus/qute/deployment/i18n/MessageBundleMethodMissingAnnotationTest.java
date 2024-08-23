package io.quarkus.qute.deployment.i18n;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.deployment.MessageBundleException;
import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;
import io.quarkus.test.QuarkusUnitTest;

public class MessageBundleMethodMissingAnnotationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBundle.class))
            .setExpectedException(MessageBundleException.class);

    @Test
    public void testValidation() {
        fail();
    }

    @MessageBundle
    public interface MyBundle {

        @Message("Hello {name}")
        String hello(String name);

        // this fails because there is no value specified in a localized file
        String notAllowed();

    }

}
