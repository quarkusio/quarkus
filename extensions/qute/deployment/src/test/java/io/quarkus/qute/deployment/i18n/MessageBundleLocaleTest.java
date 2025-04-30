package io.quarkus.qute.deployment.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;
import io.quarkus.test.QuarkusUnitTest;

public class MessageBundleLocaleTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Messages.class)
                    .addAsResource(new StringAsset(
                            "{MessageBundleLocaleTest_Messages:helloWorld}"),
                            "templates/foo.html"));

    @Inject
    Template foo;

    @Test
    public void testResolvers() {
        assertEquals("Ahoj svete!", foo.instance().setLocale("cs").render());
    }

    @MessageBundle(locale = "cs")
    public interface Messages {

        @Message("Ahoj svete!")
        String helloWorld();

    }

}
