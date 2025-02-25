package io.quarkus.qute.deployment.exclude;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.qute.Engine;
import io.quarkus.qute.deployment.TemplatePathExcludeBuildItem;
import io.quarkus.test.QuarkusUnitTest;

public class TemplatePathExcludeBuildItemTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addAsResource(new StringAsset("{@String name} Hi {name.nonexistent}!"), "templates/hi.txt")
                    .addAsResource(new StringAsset("Hello {name}!"), "templates/hello.txt"))
            .addBuildChainCustomizer(buildCustomizer());

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {
            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(new TemplatePathExcludeBuildItem("hi.txt"));
                    }
                }).produces(TemplatePathExcludeBuildItem.class)
                        .build();

            }
        };
    }

    @Inject
    Engine engine;

    @Test
    public void testTemplate() {
        assertNull(engine.getTemplate("hi"));
        assertEquals("Hello M!", engine.getTemplate("hello").data("name", "M").render());
    }

}
