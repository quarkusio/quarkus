package io.quarkus.qute.resteasy.deployment;

import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateException;
import io.quarkus.test.QuarkusUnitTest;

public class MissingTemplateTest {

    @RegisterExtension
    static final QuarkusUnitTest configError = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(MissingTemplateResource.class)
                    .addAsResource("templates/MissingTemplateResource/hello.txt"))
            .setExpectedException(TemplateException.class);

    @Test
    public void emptyTest() {
        fail();
    }
}
