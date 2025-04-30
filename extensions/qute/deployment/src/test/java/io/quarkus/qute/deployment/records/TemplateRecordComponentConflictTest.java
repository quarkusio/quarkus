package io.quarkus.qute.deployment.records;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.assertj.core.util.Throwables;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusUnitTest;

public class TemplateRecordComponentConflictTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Hello.class)
                    .addAsResource(new StringAsset("Hello {name}!"), "templates/TemplateRecordComponentConflictTest/Hello.txt"))
            .assertException(t -> {
                Throwable root = Throwables.getRootCause(t);
                if (root == null) {
                    root = t;
                }
                assertThat(root)
                        .isInstanceOf(TemplateException.class)
                        .hasMessageStartingWith("Template record component [render] conflicts with an interface method of");
            });

    @Test
    public void testValidation() {
        fail();
    }

    public record Hello(String render) implements TemplateInstance {
    }

}
