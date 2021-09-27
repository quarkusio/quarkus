package io.quarkus.qute.deployment.scanning;

import static org.junit.jupiter.api.Assertions.fail;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateException;
import io.quarkus.test.QuarkusUnitTest;

public class WrongTemplatesDirectoryTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset("Hello!"), "Templates/bing.html"))
            .setExpectedException(TemplateException.class);

    @Inject
    Template bing;

    @Test
    public void testValidation() {
        fail();
    }

}
