package io.quarkus.vertx.http;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.deployment.spi.GeneratedStaticResourceBuildItem;

public class GeneratedStaticResourceBuildItemDuplicatedTest {

    @RegisterExtension
    final static QuarkusUnitTest test = new QuarkusUnitTest().withApplicationRoot(
            (jar) -> jar.add(new StringAsset(""), "application.properties"))
            .addBuildChainCustomizer(new Consumer<BuildChainBuilder>() {
                @Override
                public void accept(BuildChainBuilder buildChainBuilder) {

                    buildChainBuilder.addBuildStep(new BuildStep() {
                        @Override
                        public void execute(BuildContext context) {
                            context.produce(new GeneratedStaticResourceBuildItem(
                                    "/index.html", "Hello from Quarkus".getBytes(StandardCharsets.UTF_8)));
                            context.produce(new GeneratedStaticResourceBuildItem("/index.html",
                                    "GeneratedStaticResourceBuildItem says: Hello from me!".getBytes(StandardCharsets.UTF_8)));
                        }
                    }).produces(GeneratedStaticResourceBuildItem.class).produces(GeneratedResourceBuildItem.class).build();
                }
            })
            .assertException(throwable -> {
                String message = throwable.getCause().getMessage();
                assertThat(message)
                        .contains("Duplicate endpoints detected, the endpoint for static resources must be unique:");
                assertThat(message).contains("/index.html");
            });

    @Test
    void assertTrue() {
        Assertions.assertTrue(true);
    }
}
