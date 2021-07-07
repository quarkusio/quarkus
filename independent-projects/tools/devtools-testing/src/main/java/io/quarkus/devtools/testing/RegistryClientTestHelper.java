package io.quarkus.devtools.testing;

import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.devtools.testing.registry.client.TestRegistryClientBuilder;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.config.RegistriesConfigLocator;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class RegistryClientTestHelper {

    public static void enableRegistryClientTestConfig() {
        enableRegistryClientTestConfig(System.getProperties());
    }

    public static void enableRegistryClientTestConfig(Properties properties) {
        enableRegistryClientTestConfig(Paths.get("").normalize().toAbsolutePath().resolve("target"),
                properties);
    }

    public static void enableRegistryClientTestConfig(Path outputDir, Properties properties) {
        final String projectVersion = System.getProperty("project.version");
        if (projectVersion == null) {
            throw new IllegalStateException("System property project.version isn't set");
        }

        final Path toolsConfigPath = outputDir.resolve(RegistriesConfigLocator.CONFIG_RELATIVE_PATH);

        final ArtifactCoords bom = new ArtifactCoords("io.quarkus", "quarkus-bom", null, "pom", projectVersion);

        QuarkusProjectHelper.resetToolsConfig();
        TestRegistryClientBuilder.newInstance()
                .baseDir(toolsConfigPath.getParent())
                //.debug()
                .newRegistry("test.quarkus.registry")
                .newPlatform(bom.getGroupId())
                .newStream(projectVersion)
                .newRelease(projectVersion)
                .quarkusVersion(projectVersion)
                .addMemberBom(bom)
                .registry()
                .clientBuilder()
                .build();

        properties.setProperty(RegistriesConfigLocator.CONFIG_FILE_PATH_PROPERTY, toolsConfigPath.toString());
        properties.setProperty("quarkusRegistryClient", "true");
    }

    public static void disableRegistryClientTestConfig() {
        disableRegistryClientTestConfig(System.getProperties());
    }

    public static void disableRegistryClientTestConfig(Properties properties) {
        properties.remove(RegistriesConfigLocator.CONFIG_FILE_PATH_PROPERTY);
        properties.remove("quarkusRegistryClient");
    }
}
