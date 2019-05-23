package io.quarkus.mailer;

import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.vertx.ext.mail.MailClient;

public class InjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(BeanUsingAxleMailClient.class, BeanUsingBareMailClient.class, BeanUsingRxClient.class)
                    .addClasses(BeanUsingBlockingMailer.class, BeanUsingReactiveMailer.class)
                    .addAsResource("mock-config.properties", "application.properties"));

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

    @Test
    public void testInjection() {
        beanUsingAxle.verify();
        beanUsingBare.verify();
        beanUsingRx.verify();
        beanUsingBlockingMailer.verify();
        beanUsingReactiveMailer.verify().toCompletableFuture().join();
    }

    @ApplicationScoped
    static class BeanUsingBareMailClient {

        @Inject
        MailClient client;

        public void verify() {
            Assertions.assertNotNull(client);
        }
    }

    @ApplicationScoped
    static class BeanUsingAxleMailClient {

        @Inject
        io.vertx.axle.ext.mail.MailClient client;

        public void verify() {
            Assertions.assertNotNull(client);
        }
    }

    @ApplicationScoped
    static class BeanUsingRxClient {

        @Inject
        io.vertx.reactivex.ext.mail.MailClient client;

        public void verify() {
            Assertions.assertNotNull(client);
        }
    }

    @ApplicationScoped
    static class BeanUsingReactiveMailer {

        @Inject
        ReactiveMailer mailer;

        public CompletionStage<Void> verify() {
            return mailer.send(Mail.withText("quarkus@quarkus.io", "test mailer", "reactive test!"));
        }
    }

    @ApplicationScoped
    static class BeanUsingBlockingMailer {

        @Inject
        Mailer mailer;

        public void verify() {
            mailer.send(Mail.withText("quarkus@quarkus.io", "test mailer", "blocking test!"));
        }
    }
}
