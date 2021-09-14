package io.quarkus.mailer.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MailTemplate.MailTemplateInstance;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.test.QuarkusUnitTest;

public class MailMessageBundleTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Templates.class, AppMessages.class)
                    .addAsResource("mock-config.properties", "application.properties")
                    .addAsResource(new StringAsset(
                            "hello_name=Hallo {name}!"),
                            "messages/msg_de.properties")
                    .addAsResource(new StringAsset(""
                            + "{msg:hello_name(name)}"), "templates/MailMessageBundleTest/hello.txt"));

    @Inject
    MockMailbox mailbox;

    @Test
    public void testSend() {
        mailbox.clear();

        Templates.hello("Lu").to("quarkus@quarkus.io").subject("Test").send().await().indefinitely();

        List<Mail> sent = mailbox.getMessagesSentTo("quarkus@quarkus.io");
        assertEquals(1, sent.size());
        Mail english = sent.get(0);
        assertEquals("Test", english.getSubject());
        assertEquals("Hello Lu!", english.getText());

        // Set the locale attribute
        Templates.hello("Lu").to("quarkus@quarkus.io").subject("Test").setAttribute("locale", Locale.GERMAN).send().await()
                .indefinitely();

        assertEquals(2, sent.size());
        Mail german = sent.get(1);
        assertEquals("Test", german.getSubject());
        assertEquals("Hallo Lu!", german.getText());
    }

    @CheckedTemplate
    static class Templates {

        static native MailTemplateInstance hello(String name);

    }

}
