package io.quarkus.qute.deployment.i18n;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.deployment.MessageBundleException;
import io.quarkus.test.QuarkusUnitTest;

public class MessageBundleNameConflictTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(AppMessages.class, BravoMessages.class))
            .setExpectedException(MessageBundleException.class);

    @Test
    public void testValidation() {
        fail();
    }

}
