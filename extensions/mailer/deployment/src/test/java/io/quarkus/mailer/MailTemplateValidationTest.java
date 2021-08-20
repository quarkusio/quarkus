package io.quarkus.mailer;

import javax.enterprise.inject.spi.DeploymentException;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Unremovable;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class MailTemplateValidationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MailTemplates.class)
                    .addAsResource("mock-config.properties", "application.properties")
                    .addAsResource(new StringAsset(""
                            + "<html>{name}</html>"), "templates/test1.html"))
            .setExpectedException(DeploymentException.class);

    @Test
    public void testValidationFailed() {
        // This method should not be invoked
        Assertions.fail();
    }

    @Unremovable // Injection points from removed beans are not validated
    @Singleton
    static class MailTemplates {

        @Inject
        MailTemplate doesNotExist;

        Uni<Void> send() {
            return doesNotExist.to("quarkus@quarkus.io").subject("Test").data("name", "Foo").send();
        }

    }
}
