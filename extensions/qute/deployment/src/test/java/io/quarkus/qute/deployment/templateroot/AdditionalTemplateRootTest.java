package io.quarkus.qute.deployment.templateroot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import io.quarkus.qute.deployment.TemplateRootBuildItem;
import io.quarkus.test.QuarkusUnitTest;

public class AdditionalTemplateRootTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addAsResource(new StringAsset("Hi {name}!"), "templates/hi.txt")
                    .addAsResource(new StringAsset("Hoho {name}!"), "templates/nested/hoho.txt")
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

                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        int found = 0;
                        List<NativeImageResourceBuildItem> items = context.consumeMulti(NativeImageResourceBuildItem.class);
                        for (NativeImageResourceBuildItem item : items) {
                            if (item.getResources().contains("web/public/hello.txt")
                                    || item.getResources().contains("templates/hi.txt")
                                    || item.getResources().contains("templates/nested/hoho.txt")) {
                                found++;
                            }
                        }
                        if (found != 3) {
                            throw new IllegalStateException(items.stream().flatMap(i -> i.getResources().stream())
                                    .collect(Collectors.toList()).toString());
                        }
                        context.produce(new ServiceStartBuildItem("foo"));
                    }
                }).produces(ServiceStartBuildItem.class)
                        .consumes(NativeImageResourceBuildItem.class)
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
        assertEquals("Hoho M!", engine.getTemplate("nested/hoho").data("name", "M").render());
    }

}
