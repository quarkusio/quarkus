package io.quarkus.mailer;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Location;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.mail.MailMessage;

public class NamedMailerTemplateOnlyInjectionTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Templates.class, ConstructorTemplates.class)
                    .addAsResource("mock-config-named-mailers.properties", "application.properties")
                    .addAsResource(new StringAsset("<html>{name}</html>"), "templates/mails/test2.html"));

    @Inject
    Templates templates;

    @Inject
    ConstructorTemplates constructorTemplates;

    @Inject
    @Any
    Instance<MockMailbox> mailboxes;

    @Test
    void namedTemplateUsesItsMailer() {
        templates.send().await().indefinitely();

        MockMailbox mailbox = mailboxes.select(MailerName.Literal.of("client1")).get();
        Assertions.assertThat(mailbox.getMailMessagesSentTo("only-template@quarkus.io"))
                .singleElement()
                .extracting(MailMessage::getFrom)
                .isEqualTo("from-client1@quarkus.io");
    }

    @Test
    void constructorParameterUsesItsMailer() {
        constructorTemplates.send().await().indefinitely();

        MockMailbox mailbox = mailboxes.select(MailerName.Literal.of("client1")).get();
        Assertions.assertThat(mailbox.getMailMessagesSentTo("constructor-template@quarkus.io"))
                .singleElement()
                .extracting(MailMessage::getFrom)
                .isEqualTo("from-client1@quarkus.io");
    }

    @Singleton
    static class ConstructorTemplates {

        private final MailTemplate testMail;

        @Inject
        ConstructorTemplates(@MailerName("client1") @Location("mails/test2") MailTemplate testMail) {
            this.testMail = testMail;
        }

        Uni<Void> send() {
            return testMail.to("constructor-template@quarkus.io").subject("Test").data("name", "Ada").send();
        }
    }

    @Singleton
    static class Templates {

        @Inject
        @MailerName("client1")
        @Location("mails/test2")
        MailTemplate testMail;

        Uni<Void> send() {
            return testMail.to("only-template@quarkus.io").subject("Test").data("name", "Ada").send();
        }
    }
}
