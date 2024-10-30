package io.quarkus.qute.deployment.builditemtemplate;

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
import io.quarkus.qute.deployment.TemplatePathBuildItem;
import io.quarkus.test.QuarkusUnitTest;

public class AdditionalTemplatePathTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addAsResource(new StringAsset("Hi {name}!"), "templates/hi.txt")
                    .addAsResource(new StringAsset("And... {#include foo/hello /}"), "templates/include.txt"))
            .addBuildChainCustomizer(buildCustomizer());

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {
            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(TemplatePathBuildItem.builder()
                                .path("foo/hello.txt")
                                .extensionInfo("test-ext")
                                .content("Hello {name}!").build());
                    }
                }).produces(TemplatePathBuildItem.class)
                        .build();

            }
        };
    }

    @Inject
    Engine engine;

    @Test
    public void testTemplate() {
        assertEquals("Hi M!", engine.getTemplate("hi").data("name", "M").render());
        assertEquals("Hello M!", engine.getTemplate("foo/hello.txt").data("name", "M").render());
        assertEquals("Hello M!", engine.getTemplate("foo/hello").data("name", "M").render());
        assertEquals("And... Hello M!", engine.getTemplate("include").data("name", "M").render());

        // Test that reload works for additional content-based paths
        engine.clearTemplates();
        assertEquals("Hello M!", engine.getTemplate("foo/hello").data("name", "M").render());
        assertEquals("Hello M!", engine.getTemplate("foo/hello.txt").data("name", "M").render());
    }

}
