package io.quarkus.mailer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.mailer.MailTemplate.MailTemplateInstance;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.ext.mail.MailMessage;

public class MailTemplateRecordNoInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(confirmation.class)
                    .addAsResource("mock-config.properties", "application.properties")
                    .addAsResource(new StringAsset(""
                            + "<html>{name}</html>"), "templates/MailTemplateRecordNoInjectionTest/confirmation.html"));

    @Test
    public void testMailTemplateRecord() {
        // Intentionally use programmatic lookup to obtain the MockMailbox
        MockMailbox mockMailbox = Arc.container().instance(MockMailbox.class).get();
        new confirmation("Ondrej").to("quarkus-reactive@quarkus.io").from("from-record@quarkus.io").subject("test mailer")
                .sendAndAwait();
        assertEquals(1, mockMailbox.getMailMessagesSentTo("quarkus-reactive@quarkus.io").size());
        MailMessage message = mockMailbox.getMailMessagesSentTo("quarkus-reactive@quarkus.io").get(0);
        assertEquals("from-record@quarkus.io", message.getFrom());
        assertEquals("<html>Ondrej</html>", message.getHtml());
    }

    record confirmation(String name) implements MailTemplateInstance {
    }

}
