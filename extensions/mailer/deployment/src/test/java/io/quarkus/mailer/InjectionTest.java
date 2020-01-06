package io.quarkus.mailer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.mailer.mutiny.ReactiveMailer;
import io.quarkus.qute.api.ResourcePath;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.mail.MailClient;

@SuppressWarnings("WeakerAccess")
public class InjectionTest {

    @SuppressWarnings("unused")
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(BeanUsingAxleMailClient.class, BeanUsingBareMailClient.class, BeanUsingRxClient.class)
                    .addClasses(BeanUsingMutinyMailClient.class, BeanUsingBareMailClient.class, BeanUsingRxClient.class)
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
    BeanUsingMutinyMailClient beanUsingMutiny;

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
        beanUsingMutiny.verify();
        beanUsingBare.verify();
        beanUsingRx.verify();
        beanUsingBlockingMailer.verify();
        beanUsingReactiveMailer.verify().await().indefinitely();
        templates.send1().await().indefinitely();
        templates.send2().await().indefinitely();
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
    static class BeanUsingMutinyMailClient {

        @Inject
        io.vertx.mutiny.ext.mail.MailClient client;

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

        Uni<Void> verify() {
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

        Uni<Void> send1() {
            return test1.to("quarkus@quarkus.io").subject("Test").data("name", "John").send();
        }

        Uni<Void> send2() {
            return testMail.to("quarkus@quarkus.io").subject("Test").data("name", "Lu").send();
        }

    }
}
