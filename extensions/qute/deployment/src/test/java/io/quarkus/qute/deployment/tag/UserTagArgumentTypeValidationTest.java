package io.quarkus.qute.deployment.tag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.assertj.core.util.Throwables;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateException;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * The arguments of a user tag call are validated against the parameter declarations of the tag template.
 */
public class UserTagArgumentTypeValidationTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("{@java.lang.Integer count}{@java.lang.Integer it}{count}{it}"),
                            "templates/tags/hello.txt")
                    .addAsResource(new StringAsset("{@java.lang.String str}{#hello count=str /}"), "templates/tags/outer.txt")
                    .addAsResource(new StringAsset("{@java.lang.String name}"
                            + "{#hello count=name /}"
                            + "{#hello count='ten' /}"
                            + "{#hello name /}"
                            + "{#outer str=name /}"), "templates/foo.txt"))
            .assertException(t -> {
                Throwable root = Throwables.getRootCause(t);
                if (root == null) {
                    root = t;
                }
                assertThat(root)
                        .isInstanceOf(TemplateException.class)
                        .hasMessageContaining("Found incorrect expressions (4)")
                        .hasMessageContaining("foo.txt:1:")
                        .hasMessageContaining("tags/outer.txt:1:")
                        .hasMessageContaining("argument [count] of the user tag [hello]")
                        .hasMessageContaining("argument [it] of the user tag [hello]")
                        .hasMessageContaining("[java.lang.String]")
                        .hasMessageContaining("[java.lang.Integer]");
            });

    @Test
    public void test() {
        fail();
    }

}
