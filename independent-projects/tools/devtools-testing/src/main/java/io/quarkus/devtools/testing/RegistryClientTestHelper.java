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

    public static void enableRegistryClientTestConfig(String quarkusBomGroupId, String quarkusBomVersion) {
        enableRegistryClientTestConfig(getConfigBaseDir(), System.getProperties(), quarkusBomGroupId, quarkusBomVersion);
    }

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
        final String quarkusBomVersion = System.getProperty("project.version");
        final String quarkusBomGroupId = System.getProperty("project.groupId");
        enableRegistryClientTestConfig(outputDir, properties, quarkusBomGroupId, quarkusBomVersion);
    }

    public static void enableRegistryClientTestConfig(Path outputDir, Properties properties, String quarkusBomGroupId,
            String quarkusBomVersion) {
        if (quarkusBomVersion == null) {
            throw new IllegalStateException("quarkusBomVersion isn't set");
        }
        if (quarkusBomGroupId == null) {
            throw new IllegalStateException("quarkusBomGroupId isn't set");
        }

        final Path toolsConfigPath = outputDir.resolve(RegistriesConfigLocator.CONFIG_RELATIVE_PATH);

        final ArtifactCoords bom = new ArtifactCoords(quarkusBomGroupId, "quarkus-bom", null, "pom", quarkusBomVersion);

        TestRegistryClientBuilder.newInstance()
                .baseDir(toolsConfigPath.getParent())
                //.debug()
                .newRegistry("test.quarkus.registry")
                .newPlatform(bom.getGroupId())
                .newStream(quarkusBomVersion)
                .newRelease(quarkusBomVersion)
                .quarkusVersion(quarkusBomVersion)
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
