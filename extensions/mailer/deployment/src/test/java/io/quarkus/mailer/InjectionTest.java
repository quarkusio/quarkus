package io.quarkus.mailer;

import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.api.ResourcePath;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.ext.mail.MailClient;

@SuppressWarnings("WeakerAccess")
public class InjectionTest {

    @SuppressWarnings("unused")
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(BeanUsingAxleMailClient.class, BeanUsingBareMailClient.class, BeanUsingRxClient.class)
                    .addClasses(BeanUsingBlockingMailer.class, BeanUsingReactiveMailer.class)
                    .addClasses(MailTemplates.class)
                    .addAsResource("mock-config.properties", "application.properties")
                    .addAsResource(new StringAsset(""
                            + "<html>{name}</html>"), "templates/test1.html")
                    .addAsResource(new StringAsset(""
                            + "{name}"), "templates/test1.txt")
                    .addAsResource(new StringAsset(""
                            + "<html>{name}</html>"), "templates/mails/test2.html"));

    @Inject
    BeanUsingAxleMailClient beanUsingBare;

    @Inject
    BeanUsingBareMailClient beanUsingAxle;

    @Inject
    BeanUsingRxClient beanUsingRx;

    @Inject
    BeanUsingReactiveMailer beanUsingReactiveMailer;

    @Inject
    BeanUsingBlockingMailer beanUsingBlockingMailer;

    @Inject
    MailTemplates templates;

    @Test
    public void testInjection() {
        beanUsingAxle.verify();
        beanUsingBare.verify();
        beanUsingRx.verify();
        beanUsingBlockingMailer.verify();
        beanUsingReactiveMailer.verify();
        templates.send1();
        templates.send2().toCompletableFuture().join();
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
    static class BeanUsingAxleMailClient {

        @Inject
        io.vertx.axle.ext.mail.MailClient client;

        void verify() {
            Assertions.assertNotNull(client);
        }
    }

    @ApplicationScoped
    static class BeanUsingRxClient {

        @Inject
        io.vertx.reactivex.ext.mail.MailClient client;

        void verify() {
            Assertions.assertNotNull(client);
        }
    }

    @ApplicationScoped
    static class BeanUsingReactiveMailer {

        @Inject
        ReactiveMailer mailer;

        CompletionStage<Void> verify() {
            return mailer.send(Mail.withText("quarkus@quarkus.io", "test mailer", "reactive test!"));
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

        @ResourcePath("mails/test2")
        MailTemplate testMail;

        CompletionStage<Void> send1() {
            return test1.to("quarkus@quarkus.io").subject("Test").data("name", "John").send();
        }

        CompletionStage<Void> send2() {
            return testMail.to("quarkus@quarkus.io").subject("Test").data("name", "Lu").send();
        }

    }
}
