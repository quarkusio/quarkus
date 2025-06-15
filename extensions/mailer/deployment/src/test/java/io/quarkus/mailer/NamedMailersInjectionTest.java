package io.quarkus.mailer;

import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Location;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.mail.MailClient;

public class NamedMailersInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addClasses(BeanUsingBareMailClient.class, BeanUsingMutinyClient.class, BeanUsingBlockingMailer.class,
                    BeanUsingReactiveMailer.class, MailTemplates.class, BeanUsingBareMailClientNamedClient1.class,
                    BeanUsingMutinyClientNamedClient1.class, BeanUsingReactiveMailerNamedClient1.class,
                    BeanUsingBlockingMailerNamedClient1.class, BeanUsingBareMailClientNamedClient2.class,
                    BeanUsingMutinyClientNamedClient2.class, BeanUsingReactiveMailerNamedClient2.class,
                    BeanUsingBlockingMailerNamedClient2.class)
            .addAsResource("mock-config-named-mailers.properties", "application.properties"));

    @Inject
    BeanUsingBareMailClient beanUsingBare;

    @Inject
    BeanUsingMutinyClient beanUsingMutiny;

    @Inject
    BeanUsingReactiveMailer beanUsingReactiveMailer;

    @Inject
    BeanUsingBlockingMailer beanUsingBlockingMailer;

    @Inject
    MockMailbox mockMailbox;

    @Inject
    BeanUsingBareMailClientNamedClient1 beanUsingBareNamedClient1;

    @Inject
    BeanUsingMutinyClientNamedClient1 beanUsingMutinyNamedClient1;

    @Inject
    BeanUsingReactiveMailerNamedClient1 beanUsingReactiveMailerNamedClient1;

    @Inject
    BeanUsingBlockingMailerNamedClient1 beanUsingBlockingMailerNamedClient1;

    @Inject
    @MailerName("client1")
    MockMailbox mockMailboxNamedClient1;

    @Inject
    BeanUsingBareMailClientNamedClient2 beanUsingBareNamedClient2;

    @Inject
    BeanUsingMutinyClientNamedClient2 beanUsingMutinyNamedClient2;

    @Inject
    BeanUsingReactiveMailerNamedClient2 beanUsingReactiveMailerNamedClient2;

    @Inject
    BeanUsingBlockingMailerNamedClient2 beanUsingBlockingMailerNamedClient2;

    @Inject
    @MailerName("client2")
    MockMailbox mockMailboxNamedClient2;

    @Test
    public void testInjection() {
        beanUsingMutiny.verify();
        beanUsingBare.verify();
        beanUsingBlockingMailer.verify();
        beanUsingReactiveMailer.verify();

        beanUsingMutinyNamedClient1.verify();
        beanUsingBareNamedClient1.verify();
        beanUsingBlockingMailerNamedClient1.verify();
        beanUsingReactiveMailerNamedClient1.verify();

        beanUsingMutinyNamedClient2.verify();
        beanUsingBareNamedClient2.verify();
        beanUsingBlockingMailerNamedClient2.verify();
        beanUsingReactiveMailerNamedClient2.verify();
    }

    @ApplicationScoped
    static class BeanUsingBareMailClient {

        @Inject
        MailClient client;

        void verify() {
            Assertions.assertNotNull(client);
        }
    }

    @ApplicationScoped
    static class BeanUsingMutinyClient {

        @Inject
        io.vertx.mutiny.ext.mail.MailClient client;

        void verify() {
            Assertions.assertNotNull(client);
        }
    }

    @ApplicationScoped
    static class BeanUsingReactiveMailer {

        @Inject
        io.quarkus.mailer.reactive.ReactiveMailer mailer;

        CompletionStage<Void> verify() {
            return mailer.send(Mail.withText("quarkus@quarkus.io", "test mailer", "reactive test!"))
                    .subscribeAsCompletionStage();
        }
    }

    @ApplicationScoped
    static class BeanUsingBlockingMailer {

        @Inject
        Mailer mailer;

        void verify() {
            mailer.send(Mail.withText("quarkus@quarkus.io", "test mailer", "blocking test!"));
        }
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

    @ApplicationScoped
    static class BeanUsingBareMailClientNamedClient1 {

        @Inject
        @MailerName("client1")
        MailClient client;

        void verify() {
            Assertions.assertNotNull(client);
        }
    }

    @ApplicationScoped
    static class BeanUsingMutinyClientNamedClient1 {

        @Inject
        @MailerName("client1")
        io.vertx.mutiny.ext.mail.MailClient client;

        void verify() {
            Assertions.assertNotNull(client);
        }
    }

    @ApplicationScoped
    static class BeanUsingReactiveMailerNamedClient1 {

        @Inject
        @MailerName("client1")
        io.quarkus.mailer.reactive.ReactiveMailer mailer;

        @Inject
        @MailerName("client1")
        MockMailbox mockMailbox;

        void verify() {
            mailer.send(Mail.withText("quarkus-reactive@quarkus.io", "test mailer", "reactive test!"))
                    .subscribeAsCompletionStage().toCompletableFuture().join();

            Assertions.assertEquals(1, mockMailbox.getMailMessagesSentTo("quarkus-reactive@quarkus.io").size());
            Assertions.assertEquals("from-client1@quarkus.io",
                    mockMailbox.getMailMessagesSentTo("quarkus-reactive@quarkus.io").get(0).getFrom());
        }
    }

    @ApplicationScoped
    static class BeanUsingBlockingMailerNamedClient1 {

        @Inject
        @MailerName("client1")
        Mailer mailer;

        @Inject
        @MailerName("client1")
        MockMailbox mockMailbox;

        void verify() {
            mailer.send(Mail.withText("quarkus@quarkus.io", "test mailer", "blocking test!"));

            Assertions.assertEquals(1, mockMailbox.getMailMessagesSentTo("quarkus@quarkus.io").size());
            Assertions.assertEquals("from-client1@quarkus.io",
                    mockMailbox.getMailMessagesSentTo("quarkus@quarkus.io").get(0).getFrom());
        }
    }

    @ApplicationScoped
    static class BeanUsingBareMailClientNamedClient2 {

        @Inject
        @MailerName("client2")
        MailClient client;

        void verify() {
            Assertions.assertNotNull(client);
        }
    }

    @ApplicationScoped
    static class BeanUsingMutinyClientNamedClient2 {

        @Inject
        @MailerName("client2")
        io.vertx.mutiny.ext.mail.MailClient client;

        void verify() {
            Assertions.assertNotNull(client);
        }
    }

    @ApplicationScoped
    static class BeanUsingReactiveMailerNamedClient2 {

        @Inject
        @MailerName("client2")
        io.quarkus.mailer.reactive.ReactiveMailer mailer;

        @Inject
        @MailerName("client2")
        MockMailbox mockMailbox;

        void verify() {
            mailer.send(Mail.withText("quarkus-reactive@quarkus.io", "test mailer", "reactive test!"))
                    .subscribeAsCompletionStage().toCompletableFuture().join();

            Assertions.assertEquals(1, mockMailbox.getMailMessagesSentTo("quarkus-reactive@quarkus.io").size());
            Assertions.assertEquals("from-client2@quarkus.io",
                    mockMailbox.getMailMessagesSentTo("quarkus-reactive@quarkus.io").get(0).getFrom());
        }
    }

    @ApplicationScoped
    static class BeanUsingBlockingMailerNamedClient2 {

        @Inject
        @MailerName("client2")
        Mailer mailer;

        @Inject
        @MailerName("client2")
        MockMailbox mockMailbox;

        void verify() {
            mailer.send(Mail.withText("quarkus@quarkus.io", "test mailer", "blocking test!"));

            Assertions.assertEquals(1, mockMailbox.getMailMessagesSentTo("quarkus@quarkus.io").size());
            Assertions.assertEquals("from-client2@quarkus.io",
                    mockMailbox.getMailMessagesSentTo("quarkus@quarkus.io").get(0).getFrom());
        }
    }
}
