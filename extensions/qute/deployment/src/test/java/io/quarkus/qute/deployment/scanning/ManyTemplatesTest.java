package io.quarkus.qute.deployment.scanning;

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
import io.quarkus.test.QuarkusExtensionTest;

/**
 * An application with tens of thousands of templates must build: the template list must not
 * be recorded as constants of one generated class.
 */
public class ManyTemplatesTest {

    static final int COUNT = 20_000;

    static final int BUILD_ITEM_COUNT = 5_000;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> {
                for (int i = 0; i < COUNT; i++) {
                    root.addAsResource(new StringAsset("Hello " + i + "!"), "templates/many/page-" + i + ".html");
                }
            })
            .addBuildChainCustomizer(buildItemTemplates());

    static Consumer<BuildChainBuilder> buildItemTemplates() {
        return new Consumer<BuildChainBuilder>() {
            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        for (int i = 0; i < BUILD_ITEM_COUNT; i++) {
                            context.produce(TemplatePathBuildItem.builder()
                                    .path("extension/item-" + i + ".txt")
                                    .extensionInfo("test-ext")
                                    .content("Item " + i + " {name}").build());
                        }
                    }
                }).produces(TemplatePathBuildItem.class)
                        .build();
            }
        };
    }

    @Inject
    Engine engine;

    @Test
    public void testAllTemplatesFound() {
        assertEquals("Hello 0!", engine.getTemplate("many/page-0.html").render());
        assertEquals("Hello " + (COUNT - 1) + "!", engine.getTemplate("many/page-" + (COUNT - 1)).render());
    }

    @Test
    public void testAllBuildItemTemplatesFound() {
        assertEquals("Item 0 M", engine.getTemplate("extension/item-0.txt").data("name", "M").render());
        assertEquals("Item " + (BUILD_ITEM_COUNT - 1) + " M",
                engine.getTemplate("extension/item-" + (BUILD_ITEM_COUNT - 1)).data("name", "M").render());
    }

}
