package io.quarkus.qute.deployment.typesafe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.assertj.core.util.Throwables;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateException;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * The error of a property that is not found points to the column of the property, not to the start of the expression.
 */
public class IncorrectExpressionColumnTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Item.class)
                    .addAsResource(new StringAsset("{@io.quarkus.qute.deployment.typesafe.Item item}\n"
                            + "<strong>{item.pric}</strong>\n"
                            + "{#if item.name.lenght > 1}x{/if}"), "templates/foo.html"))
            .assertException(t -> {
                Throwable root = Throwables.getRootCause(t);
                if (root == null) {
                    root = t;
                }
                assertThat(root)
                        .isInstanceOf(TemplateException.class)
                        .hasMessageContaining("Found incorrect expressions (2)")
                        .hasMessageContaining("foo.html:2:15 - {item.pric}: Property/method [pric] not found")
                        .hasMessageContaining("foo.html:3:1 - {item.name.lenght}: Property/method [lenght] not found");
            });

    @Test
    public void test() {
        fail();
    }

}
