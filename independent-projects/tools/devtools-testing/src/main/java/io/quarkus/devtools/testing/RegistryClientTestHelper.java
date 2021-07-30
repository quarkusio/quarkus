package io.quarkus.devtools.testing;

import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.devtools.testing.registry.client.TestRegistryClientBuilder;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.config.RegistriesConfigLocator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class RegistryClientTestHelper {

    public static void enableRegistryClientTestConfig() {
        enableRegistryClientTestConfig(System.getProperties());
    }

    public static void enableRegistryClientTestConfig(Properties properties) {
        enableRegistryClientTestConfig(getConfigBaseDir(), properties);
    }

    private static Path getConfigBaseDir() {
        return Paths.get("").normalize().toAbsolutePath().resolve("target");
    }

    public static void enableRegistryClientTestConfig(Path outputDir, Properties properties) {
        final String projectVersion = System.getProperty("project.version");
        if (projectVersion == null) {
            throw new IllegalStateException("System property project.version isn't set");
        }
        final String projectGroupId = System.getProperty("project.groupId");
        if (projectGroupId == null) {
            throw new IllegalStateException("System property project.groupId isn't set");
        }

        final Path toolsConfigPath = outputDir.resolve(RegistriesConfigLocator.CONFIG_RELATIVE_PATH);

        final ArtifactCoords bom = new ArtifactCoords(projectGroupId, "quarkus-bom", null, "pom", projectVersion);

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

        enableExistingConfig(properties, toolsConfigPath);
    }

    public static void reenableRegistryClientTestConfig() {
        final Path toolsConfigPath = getConfigBaseDir().resolve(RegistriesConfigLocator.CONFIG_RELATIVE_PATH);
        if (Files.exists(toolsConfigPath)) {
            enableExistingConfig(System.getProperties(), toolsConfigPath);
        }
        enableRegistryClientTestConfig();
    }

    private static void enableExistingConfig(Properties properties, final Path toolsConfigPath) {
        properties.setProperty(RegistriesConfigLocator.CONFIG_FILE_PATH_PROPERTY, toolsConfigPath.toString());
        properties.setProperty("quarkusRegistryClient", "true");
        QuarkusProjectHelper.reset();
    }

    public static void disableRegistryClientTestConfig() {
        disableRegistryClientTestConfig(System.getProperties());
    }

    public static void disableRegistryClientTestConfig(Properties properties) {
        properties.remove(RegistriesConfigLocator.CONFIG_FILE_PATH_PROPERTY);
        properties.remove("quarkusRegistryClient");
    }
}
