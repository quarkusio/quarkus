package io.quarkus.vertx.http.webjar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.maven.dependency.GACT;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.http.deployment.webjar.WebJarBuildItem;
import io.quarkus.vertx.http.deployment.webjar.WebJarResultsBuildItem;
import io.quarkus.vertx.http.runtime.devmode.FileSystemStaticHandler;

/**
 * The same artifact can be deployed as a web jar with several roots (for example Dev UI resources and an
 * extension's own UI), and each root must get its own result.
 */
public class WebJarMultipleRootsTest {

    private static final GACT ARTIFACT = new GACT("io.quarkus", "quarkus-vertx-http", null, "jar");
    private static final String FIRST_ROOT = "io/quarkus/vertx/http/runtime/devmode/";
    private static final String SECOND_ROOT = "io/quarkus/vertx/http/runtime/security/";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addAsResource(new StringAsset(""), "application.properties"))
            .addBuildChainCustomizer(buildCustomizer());

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {
            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(WebJarBuildItem.builder().artifactKey(ARTIFACT).root(FIRST_ROOT).build());
                        context.produce(WebJarBuildItem.builder().artifactKey(ARTIFACT).root(SECOND_ROOT).build());
                    }
                }).produces(WebJarBuildItem.class).build();

                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        WebJarResultsBuildItem results = context.consume(WebJarResultsBuildItem.class);
                        assertRoot(results, FIRST_ROOT);
                        assertRoot(results, SECOND_ROOT);
                        assertThatThrownBy(() -> results.byArtifactKey(ARTIFACT)).isInstanceOf(IllegalStateException.class);
                        context.produce(new GeneratedResourceBuildItem("webjar-multiple-roots.txt",
                                "checked".getBytes(StandardCharsets.UTF_8)));
                    }
                }).consumes(WebJarResultsBuildItem.class).produces(GeneratedResourceBuildItem.class).build();
            }
        };
    }

    static void assertRoot(WebJarResultsBuildItem results, String root) {
        WebJarResultsBuildItem.WebJarResult result = results.byArtifactKeyAndRoot(ARTIFACT, root);
        assertThat(result).as("result for root " + root).isNotNull();
        assertThat(result.getFinalDestination()).endsWith(root.substring(0, root.length() - 1));
        assertThat(result.getWebRootConfigurations())
                .extracting(FileSystemStaticHandler.StaticWebRootConfiguration::getWebRoot)
                .contains(root);
    }

    @Test
    public void eachRootGetsItsOwnResult() {
        assertThat(config).isNotNull();
    }
}
