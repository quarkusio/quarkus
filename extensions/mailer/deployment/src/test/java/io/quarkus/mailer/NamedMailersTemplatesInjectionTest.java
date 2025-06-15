package io.quarkus.mailer;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Location;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class NamedMailersTemplatesInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addClasses(MailTemplates.class, MailTemplatesNamedClient1.class, MailTemplatesNamedClient2.class)
            .addAsResource("mock-config-named-mailers.properties", "application.properties")
            .addAsResource(new StringAsset("" + "<html>{name}</html>"), "templates/test1.html")
            .addAsResource(new StringAsset("" + "{name}"), "templates/test1.txt")
            .addAsResource(new StringAsset("" + "<html>{name}</html>"), "templates/MailTemplates/testNative.html")
            .addAsResource(new StringAsset("" + "{name}"), "templates/MailTemplates/testNative.txt")
            .addAsResource(new StringAsset("" + "<html>{name}</html>"), "templates/mails/test2.html"));

    @Inject
    MailTemplates templates;

    @Inject
    MockMailbox mockMailbox;

    @Inject
    MailTemplatesNamedClient1 templatesNamedClient1;

    @Inject
    @MailerName("client1")
    MockMailbox mockMailboxNamedClient1;

    @Inject
    MailTemplatesNamedClient2 templatesNamedClient2;

    @Inject
    @MailerName("client2")
    MockMailbox mockMailboxNamedClient2;

    @Test
    public void testInjection() {
        templates.send1().await().indefinitely();
        templates.send2().await().indefinitely();
        Assertions.assertEquals(1, mockMailbox.getMailMessagesSentTo("quarkus-send1@quarkus.io").size());
        Assertions.assertEquals("from@quarkus.io",
                mockMailbox.getMailMessagesSentTo("quarkus-send1@quarkus.io").get(0).getFrom());
        Assertions.assertEquals("<html>John</html>",
                mockMailbox.getMailMessagesSentTo("quarkus-send1@quarkus.io").get(0).getHtml());
        Assertions.assertEquals(1, mockMailbox.getMailMessagesSentTo("quarkus-send2@quarkus.io").size());
        Assertions.assertEquals("from@quarkus.io",
                mockMailbox.getMailMessagesSentTo("quarkus-send2@quarkus.io").get(0).getFrom());
        Assertions.assertEquals("<html>Lu</html>",
                mockMailbox.getMailMessagesSentTo("quarkus-send2@quarkus.io").get(0).getHtml());

        templatesNamedClient1.send1().await().indefinitely();
        templatesNamedClient1.send2().await().indefinitely();
        Assertions.assertEquals(1,
                mockMailboxNamedClient1.getMailMessagesSentTo("quarkus-template-client1-send1@quarkus.io").size());
        Assertions.assertEquals("from-client1@quarkus.io", mockMailboxNamedClient1
                .getMailMessagesSentTo("quarkus-template-client1-send1@quarkus.io").get(0).getFrom());
        Assertions.assertEquals("<html>John</html>", mockMailboxNamedClient1
                .getMailMessagesSentTo("quarkus-template-client1-send1@quarkus.io").get(0).getHtml());
        Assertions.assertEquals(1,
                mockMailboxNamedClient1.getMailMessagesSentTo("quarkus-template-client1-send2@quarkus.io").size());
        Assertions.assertEquals("from-client1@quarkus.io", mockMailboxNamedClient1
                .getMailMessagesSentTo("quarkus-template-client1-send2@quarkus.io").get(0).getFrom());
        Assertions.assertEquals("<html>Lu</html>", mockMailboxNamedClient1
                .getMailMessagesSentTo("quarkus-template-client1-send2@quarkus.io").get(0).getHtml());

        templatesNamedClient2.send1().await().indefinitely();
        templatesNamedClient2.send2().await().indefinitely();
        Assertions.assertEquals(1,
                mockMailboxNamedClient2.getMailMessagesSentTo("quarkus-template-client2-send1@quarkus.io").size());
        Assertions.assertEquals("from-client2@quarkus.io", mockMailboxNamedClient2
                .getMailMessagesSentTo("quarkus-template-client2-send1@quarkus.io").get(0).getFrom());
        Assertions.assertEquals("<html>John</html>", mockMailboxNamedClient2
                .getMailMessagesSentTo("quarkus-template-client2-send1@quarkus.io").get(0).getHtml());
        Assertions.assertEquals(1,
                mockMailboxNamedClient2.getMailMessagesSentTo("quarkus-template-client2-send2@quarkus.io").size());
        Assertions.assertEquals("from-client2@quarkus.io", mockMailboxNamedClient2
                .getMailMessagesSentTo("quarkus-template-client2-send2@quarkus.io").get(0).getFrom());
        Assertions.assertEquals("<html>Lu</html>", mockMailboxNamedClient2
                .getMailMessagesSentTo("quarkus-template-client2-send2@quarkus.io").get(0).getHtml());
    }

    @Singleton
    static class MailTemplates {

        @Inject
        MailTemplate test1;

        @Location("mails/test2")
        MailTemplate testMail;

        Uni<Void> send1() {
            return test1.to("quarkus-send1@quarkus.io").subject("Test").data("name", "John").send();
        }

        Uni<Void> send2() {
            return testMail.to("quarkus-send2@quarkus.io").subject("Test").data("name", "Lu").send();
        }
    }

    @Singleton
    static class MailTemplatesNamedClient1 {

        @Inject
        @MailerName("client1")
        MailTemplate test1;

        @Location("mails/test2")
        @MailerName("client1")
        MailTemplate testMail;

        Uni<Void> send1() {
            return test1.to("quarkus-template-client1-send1@quarkus.io").subject("Test").data("name", "John").send();
        }

        Uni<Void> send2() {
            return testMail.to("quarkus-template-client1-send2@quarkus.io").subject("Test").data("name", "Lu").send();
        }
    }

    @Singleton
    static class MailTemplatesNamedClient2 {

        @Inject
        @MailerName("client2")
        MailTemplate test1;

        @Location("mails/test2")
        @MailerName("client2")
        MailTemplate testMail;

        Uni<Void> send1() {
            return test1.to("quarkus-template-client2-send1@quarkus.io").subject("Test").data("name", "John").send();
        }

        Uni<Void> send2() {
            return testMail.to("quarkus-template-client2-send2@quarkus.io").subject("Test").data("name", "Lu").send();
        }
    }
}
