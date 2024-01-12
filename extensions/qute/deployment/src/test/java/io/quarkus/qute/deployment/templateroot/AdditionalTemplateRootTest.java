package io.quarkus.qute.deployment.templateroot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import io.quarkus.qute.deployment.TemplateRootBuildItem;
import io.quarkus.test.QuarkusUnitTest;

public class AdditionalTemplateRootTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addAsResource(new StringAsset("Hi {name}!"), "templates/hi.txt")
                    .addAsResource(new StringAsset("Hello {name}!"), "web/public/hello.txt"))
            .addBuildChainCustomizer(buildCustomizer());

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {
            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(new TemplateRootBuildItem("web/public"));
                    }
                }).produces(TemplateRootBuildItem.class)
                        .build();
            }
        };
    }

    @Inject
    Template hello;

    @Inject
    Engine engine;

    @Test
    public void testTemplate() {
        assertEquals("Hi M!", engine.getTemplate("hi").data("name", "M").render());
        assertEquals("Hello M!", hello.data("name", "M").render());
    }

}
