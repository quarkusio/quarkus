package io.quarkus.mailer;

import java.time.Duration;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Location;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class MailTemplateLocationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addClasses(MailTemplates.class).addAsResource("mock-config.properties", "application.properties")
            .addAsResource(new StringAsset("" + "<html>{name}</html>"), "templates/confirmation.html"));

    @Inject
    MailTemplates mailTemplates;

    @Test
    public void testLocation() {
        mailTemplates.send().await().atMost(Duration.ofSeconds(5));
    }

    @Singleton
    static class MailTemplates {

        @Inject
        @Location("confirmation")
        MailTemplate confirmationMailTemplate;

        Uni<Void> send() {
            return confirmationMailTemplate.to("quarkus@quarkus.io").subject("Test").data("name", "Foo").send();
        }
    }
}
