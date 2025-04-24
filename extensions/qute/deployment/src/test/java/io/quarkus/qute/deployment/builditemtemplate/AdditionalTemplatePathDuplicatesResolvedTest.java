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
import io.quarkus.qute.Template;
import io.quarkus.qute.deployment.TemplatePathBuildItem;
import io.quarkus.test.QuarkusUnitTest;

public class AdditionalTemplatePathDuplicatesResolvedTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addAsResource(new StringAsset("Hi {name}!"), "templates/hi.txt"))
            .addBuildChainCustomizer(buildCustomizer());

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {
            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(TemplatePathBuildItem.builder()
                                .path("hi.txt")
                                .extensionInfo("test-ext")
                                .content("Hello {name}!")
                                .priority(100)
                                .build());
                    }
                }).produces(TemplatePathBuildItem.class)
                        .build();

            }
        };
    }

    @Inject
    Template hi;

    @Test
    public void testHi() {
        // Build item with higher priority takes precedence
        assertEquals("Hello Lu!", hi.data("name", "Lu").render());
    }

}
