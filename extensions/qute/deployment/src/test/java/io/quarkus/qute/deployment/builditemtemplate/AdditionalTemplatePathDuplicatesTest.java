package io.quarkus.qute.deployment.builditemtemplate;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.function.Consumer;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.qute.deployment.TemplatePathBuildItem;
import io.quarkus.test.QuarkusUnitTest;

public class AdditionalTemplatePathDuplicatesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addAsResource(new StringAsset("Hi {name}!"), "templates/hi.txt"))
            .overrideConfigKey("quarkus.qute.duplicit-templates-strategy", "fail")
            .addBuildChainCustomizer(buildCustomizer())
            .setExpectedException(IllegalStateException.class, true);

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
                                .content("Hello {name}!").build());
                    }
                }).produces(TemplatePathBuildItem.class)
                        .build();

                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(TemplatePathBuildItem.builder()
                                .path("hi.txt")
                                .extensionInfo("test-ext")
                                .content("Hello {name}!").build());
                    }
                }).produces(TemplatePathBuildItem.class)
                        .build();
            }
        };
    }

    @Test
    public void test() {
        fail();
    }

}
