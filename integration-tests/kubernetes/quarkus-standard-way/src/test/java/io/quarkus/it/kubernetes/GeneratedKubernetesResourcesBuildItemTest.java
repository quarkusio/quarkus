package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildContext;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.kubernetes.spi.GeneratedKubernetesResourceBuildItem;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestBuildStep;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class GeneratedKubernetesResourcesBuildItemTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName("basic")
            .setApplicationVersion("0.1-SNAPSHOT")
            .addBuildChainCustomizerEntries(new QuarkusProdModeTest.BuildChainCustomizerEntry(
                    CustomGeneratedKubernetesResourcesHandler.class,
                    Collections.singletonList(GeneratedResourceBuildItem.class),
                    Collections.singletonList(GeneratedKubernetesResourceBuildItem.class)));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void test() {
        Path buildDir = prodModeTestResults.getBuildDir();
        assertThat(buildDir).isDirectory();
        Path quarkusAppDir = buildDir.resolve("quarkus-app");
        assertThat(quarkusAppDir).isDirectory();
        Path quarkusDir = quarkusAppDir.resolve("quarkus");
        assertThat(quarkusDir).isDirectory();
        Path generatedBytecodeJar = quarkusDir.resolve("generated-bytecode.jar");
        assertThat(generatedBytecodeJar).isRegularFile();
        try (JarFile jarFile = new JarFile(generatedBytecodeJar.toFile())) {
            assertEntry(jarFile, "dummy-kubernetes.json");
            assertEntry(jarFile, "dummy-kubernetes.yml");
        } catch (IOException e) {
            fail("Unable to verify contents of generated-bytecode jar file: " + e.getMessage());
        }
    }

    private void assertEntry(JarFile jarFile, String name) {
        JarEntry jarEntry = jarFile.getJarEntry(name);
        if (jarEntry == null) {
            fail(String.format("Unable to locate expected entry '%s' in generated-bytecode jar", name));
        }
    }

    /**
     * This simulates an extension using {@link GeneratedKubernetesResourceBuildItem}.
     * It's testable because it writes the contents of each build item to the generate-bytecode jar.
     */
    public static class CustomGeneratedKubernetesResourcesHandler extends ProdModeTestBuildStep {

        public CustomGeneratedKubernetesResourcesHandler(Map<String, Object> testContext) {
            super(testContext);
        }

        @Override
        public void execute(BuildContext context) {
            List<GeneratedKubernetesResourceBuildItem> k8sResources = context
                    .consumeMulti(GeneratedKubernetesResourceBuildItem.class);
            for (GeneratedKubernetesResourceBuildItem bi : k8sResources) {
                context.produce(new GeneratedResourceBuildItem("dummy-" + bi.getName(), bi.getContent()));
            }
        }
    }
}
