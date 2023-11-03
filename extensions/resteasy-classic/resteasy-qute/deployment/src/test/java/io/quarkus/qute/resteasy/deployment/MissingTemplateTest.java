package io.quarkus.qute.resteasy.deployment;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateException;
import io.quarkus.test.QuarkusUnitTest;

public class MissingTemplateTest {

    @RegisterExtension
    static final QuarkusUnitTest configError = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MissingTemplateResource.class)
                    .addAsResource("templates/MissingTemplateResource/hello.txt"))
            .setExpectedException(TemplateException.class);

    @Test
    public void emptyTest() {
        fail();
    }
}
