package io.quarkus.resteasy.reactive.qute.deployment;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateException;
import io.quarkus.test.QuarkusExtensionTest;

public class MissingTemplateTest {

    @RegisterExtension
    static final QuarkusExtensionTest configError = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MissingTemplateResource.class)
                    .addAsResource("templates/MissingTemplateResource/hello.txt"))
            .setExpectedException(TemplateException.class);

    @Test
    public void emptyTest() {
        fail();
    }
}
