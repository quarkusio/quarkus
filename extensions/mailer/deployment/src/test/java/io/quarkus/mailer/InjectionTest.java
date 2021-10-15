package io.quarkus.mailer;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

import io.quarkus.mailer.MailTemplate.MailTemplateInstance;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.Location;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.mail.MailClient;

@SuppressWarnings("WeakerAccess")
public class InjectionTest {

    @SuppressWarnings("unused")
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(BeanUsingBareMailClient.class, BeanUsingBlockingMailer.class,
                            BeanUsingReactiveMailer.class, MailTemplates.class)
                    .addAsResource("mock-config.properties", "application.properties")
                    .addAsResource(new StringAsset(""
                            + "<html>{name}</html>"), "templates/test1.html")
                    .addAsResource(new StringAsset(""
                            + "{name}"), "templates/test1.txt")
                    .addAsResource(new StringAsset(""
                            + "<html>{name}</html>"), "templates/MailTemplates/testNative.html")
                    .addAsResource(new StringAsset(""
                            + "{name}"), "templates/MailTemplates/testNative.txt")
                    .addAsResource(new StringAsset(""
                            + "<html>{name}</html>"), "templates/mails/test2.html"));

    @Inject
    BeanUsingBareMailClient beanUsingBare;

    @Inject
    BeanUsingMutinyClient beanUsingMutiny;

    @Inject
    BeanUsingReactiveMailer beanUsingReactiveMailer;

    @Inject
    BeanUsingBlockingMailer beanUsingBlockingMailer;

    @Inject
    MailTemplates templates;

    @Test
    public void testInjection() {
        beanUsingMutiny.verify();
        beanUsingBare.verify();
        beanUsingBlockingMailer.verify();
        beanUsingReactiveMailer.verify().toCompletableFuture().join();
        templates.send1();
        templates.send2().await();
        templates.sendNative().await();
        assertEquals("<html>Me</html>", MailTemplates.Templates.testNative("Me").templateInstance().render());
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

        @CheckedTemplate
        static class Templates {
            public static native MailTemplateInstance testNative(String name);
        }

        @Inject
        MailTemplate test1;

        @Location("mails/test2")
        MailTemplate testMail;

        Uni<Void> send1() {
            return test1.to("quarkus@quarkus.io").subject("Test").data("name", "John").send();
        }

        Uni<Void> send2() {
            return testMail.to("quarkus@quarkus.io").subject("Test").data("name", "Lu").send();
        }

        Uni<Void> sendNative() {
            return Templates.testNative("John").to("quarkus@quarkus.io").subject("Test").send();
        }
    }
}
