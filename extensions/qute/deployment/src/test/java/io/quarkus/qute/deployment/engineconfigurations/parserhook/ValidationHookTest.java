package io.quarkus.qute.deployment.engineconfigurations.parserhook;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.function.Consumer;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.qute.TemplateException;
import io.quarkus.qute.deployment.ValidationParserHookBuildItem;
import io.quarkus.test.QuarkusUnitTest;

public class ValidationHookTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(
                    root -> root.addClasses(Foo.class)
                            .addAsResource(new StringAsset("{foo.bar}"), "templates/foo.html"))
            .assertException(t -> {
                Throwable e = t;
                TemplateException te = null;
                while (e != null) {
                    if (e instanceof TemplateException) {
                        te = (TemplateException) e;
                        break;
                    }
                    e = e.getCause();
                }
                assertNotNull(te);
                assertTrue(te.getMessage().contains("Found incorrect expressions (1)"), te.getMessage());
                assertTrue(te.getMessage().contains("{foo.bar}"), te.getMessage());
            }).addBuildChainCustomizer(buildCustomizer());

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {
            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(new ValidationParserHookBuildItem(helper -> {
                            if (helper.getTemplateId().contains("foo")) {
                                helper.addParameter("foo", Foo.class.getName());
                            }
                        }));
                    }
                }).produces(ValidationParserHookBuildItem.class)
                        .build();

            }
        };
    }

    @Test
    public void test() {
        fail();
    }

    public static class Foo {

        // package-private method is ignored
        String bar() {
            return null;
        }

    }

}
