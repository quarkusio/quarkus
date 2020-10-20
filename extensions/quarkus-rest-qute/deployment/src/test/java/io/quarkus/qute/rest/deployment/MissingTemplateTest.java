package io.quarkus.qute.rest.deployment;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.wildfly.common.Assert;

import io.quarkus.test.QuarkusUnitTest;

public class MissingTemplateTest {

    @RegisterExtension
    static final QuarkusUnitTest configError = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(MissingTemplateResource.class)
                    .addAsResource("templates/MissingTemplateResource/hello.txt"))
            .assertException(t -> {
                t.printStackTrace();
                Assert.assertTrue(t.getMessage().contains(
                        "Declared template MissingTemplateResource/missingTemplate could not be found. Either add it or delete its declaration in io.quarkus.qute.rest.deployment.MissingTemplateResource$Templates.missingTemplate"));
            });

    @Test
    public void emptyTest() {
    }
}
