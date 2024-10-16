package io.quarkus.qute.deployment.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Engine;
import io.quarkus.qute.i18n.MessageBundles;
import io.quarkus.test.QuarkusUnitTest;

public class MessageBundleNameCollisionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.default-locale", "en_US")
            .withApplicationRoot((jar) -> jar
                    .addClasses(EmailBundles.class)
                    .addAsResource("messages/EmailBundles_started.properties")
                    .addAsResource("messages/EmailBundles_started_en.properties")
                    .addAsResource("messages/EmailBundles_startedValidator.properties")
                    .addAsResource("messages/EmailBundles_startedValidator_en.properties"));

    @Inject
    Engine engine;

    @Test
    public void testBundleMethodIsFound() {
        EmailBundles.startedValidator startedValidator = MessageBundles.get(EmailBundles.startedValidator.class);
        assertEquals("You will be notified with another email when it is your turn to sign.",
                startedValidator.turnEmailWillBeSent());
        assertEquals("You will be notified with another email when it is your turn to sign.",
                engine.parse("{EmailBundles_startedValidator:turnEmailWillBeSent()}").render());
    }

}
