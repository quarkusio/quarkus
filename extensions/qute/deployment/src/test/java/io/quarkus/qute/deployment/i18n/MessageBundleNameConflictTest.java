package io.quarkus.qute.deployment.i18n;

import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.deployment.MessageBundleException;
import io.quarkus.test.QuarkusUnitTest;

public class MessageBundleNameConflictTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(AppMessages.class, BravoMessages.class))
            .setExpectedException(MessageBundleException.class);

    @Test
    public void testValidation() {
        fail();
    }

}
