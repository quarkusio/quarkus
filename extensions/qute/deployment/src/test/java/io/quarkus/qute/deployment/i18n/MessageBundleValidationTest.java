package io.quarkus.qute.deployment.i18n;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.deployment.MessageBundleException;
import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;
import io.quarkus.test.QuarkusUnitTest;

public class MessageBundleValidationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Hellos.class)
                    .addAsResource(new StringAsset(
                            "hello=Hallo {foo}!\nhello_never=Ball!"),
                            "messages/msg_de.properties"))
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
                        "Message bundle method hello_never() not found on: io.quarkus.qute.deployment.i18n.MessageBundleValidationTest$Hellos"),
                        me.getMessage());
            });

    @Test
    public void testValidation() {
        fail();
    }

    @MessageBundle
    public interface Hellos {

        @Message("Hello {foo}")
        String hello(String foo);

    }

}
