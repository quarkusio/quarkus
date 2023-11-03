package io.quarkus.qute.deployment.i18n;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.deployment.MessageBundleException;
import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;
import io.quarkus.test.QuarkusUnitTest;

public class MessageBundleInvalidNamespaceTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Hellos.class))
            .assertException(t -> {
                Throwable e = t;
                MessageBundleException me = null;
                while (e != null) {
                    if (e instanceof MessageBundleException) {
                        me = (MessageBundleException) e;
                        break;
                    }
                    e = e.getCause();
                }
                if (me == null) {
                    fail("No message bundle exception thrown: " + t);
                }
                assertTrue(me.getMessage().contains(
                        "Message bundle name [foo::] declared on io.quarkus.qute.deployment.i18n.MessageBundleInvalidNamespaceTest$Hellos must be a valid namespace"),
                        me.getMessage());
            });

    @Test
    public void testValidation() {
        fail();
    }

    @MessageBundle("foo::")
    public interface Hellos {

        @Message("Hello {foo}")
        String hello(String foo);

    }

}
