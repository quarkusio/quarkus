package io.quarkus.qute.deployment.records;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.assertj.core.util.Throwables;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusUnitTest;

public class TemplateRecordConflictTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(monkFoo.class, monk_foo.class)
                    .addAsResource(new StringAsset("Hello {name}!"), "templates/TemplateRecordConflictTest/monk_foo.txt"))
            .assertException(t -> {
                Throwable root = Throwables.getRootCause(t);
                if (root == null) {
                    root = t;
                }
                assertThat(root)
                        .isInstanceOf(TemplateException.class)
                        .hasMessageStartingWith(
                                "Multiple checked templates exist for the template path TemplateRecordConflictTest/monk_foo");
            });

    @Test
    public void testValidation() {
        fail();
    }

    @CheckedTemplate(defaultName = CheckedTemplate.UNDERSCORED_ELEMENT_NAME)
    public record monkFoo(int val) implements TemplateInstance {
    }

    public record monk_foo(String val) implements TemplateInstance {
    }

}
