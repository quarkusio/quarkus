package io.quarkus.qute.deployment.tag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.assertj.core.util.Throwables;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateException;
import io.quarkus.test.QuarkusUnitTest;

public class UserTagArgumentsValidationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(
                    (jar) -> jar.addAsResource(new StringAsset("{_args.sizes}"), "templates/tags/hello.txt")
                            .addAsResource(new StringAsset("{#hello name=val /}"), "templates/foo.txt"))
            .assertException(t -> {
                Throwable root = Throwables.getRootCause(t);
                if (root == null) {
                    root = t;
                }
                assertThat(root).isInstanceOf(TemplateException.class)
                        .hasMessageContaining("Found incorrect expressions (1)").hasMessageContaining("{_args.sizes}");
            });

    @Test
    public void test() {
        fail();
    }

}
