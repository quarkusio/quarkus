package io.quarkus.maven;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.devtools.testing.registry.client.TestRegistryClientBuilder;
import io.quarkus.registry.ExtensionCatalogResolver;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.config.RegistriesConfigLocator;

public class CreateProjectMojoTest {

    private static final String PLATFORM_KEY = "io.test.platform";

    private static Path configDir;
    private static String prevConfigPath;
    private static String prevRegistryClient;
    private static ExtensionCatalogResolver catalogResolver;

    private final AbstractMojo dummyMojo = new AbstractMojo() {
        @Override
        public void execute() throws MojoExecutionException, MojoFailureException {
        }
    };

    @BeforeAll
    static void setup() throws Exception {
        configDir = Path.of("target/test-classes/registry-client");

        TestRegistryClientBuilder.newInstance()
                .baseDir(configDir)
                .newRegistry("registry.test.io")
                .newPlatform(PLATFORM_KEY)
                .newStream("2.0")
                .newRelease("2.0.1")
                .quarkusVersion("2.0.1")
                .addCoreMember().release()
                .stream().platform()
                .newStream("1.0")
                .newRelease("1.0.5")
                .quarkusVersion("1.0.5")
                .addCoreMember().release()
                .registry()
                .clientBuilder()
                .build();

        prevConfigPath = System.setProperty(RegistriesConfigLocator.CONFIG_FILE_PATH_PROPERTY,
                configDir.resolve("config.yaml").toString());
        prevRegistryClient = System.setProperty("quarkusRegistryClient", "true");
        QuarkusProjectHelper.reset();

        catalogResolver = QuarkusProjectHelper.getCatalogResolver();
    }

    @AfterAll
    static void cleanup() {
        resetProperty(RegistriesConfigLocator.CONFIG_FILE_PATH_PROPERTY, prevConfigPath);
        resetProperty("quarkusRegistryClient", prevRegistryClient);
    }

    private static void resetProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }

    @Test
    void streamSelectsNonDefaultStream() throws Exception {
        ExtensionCatalog defaultCatalog = catalogResolver.resolveExtensionCatalog();
        assertThat(defaultCatalog.getQuarkusCoreVersion()).isEqualTo("2.0.1");

        ExtensionCatalog catalog = CreateProjectMojo.resolveExtensionsCatalog(
                dummyMojo, null, null, null, "1.0",
                catalogResolver, null, null);
        assertThat(catalog.getQuarkusCoreVersion()).isEqualTo("1.0.5");
    }

    @Test
    void streamWithPlatformKeyResolvesToCorrectVersion() throws Exception {
        ExtensionCatalog catalog = CreateProjectMojo.resolveExtensionsCatalog(
                dummyMojo, null, null, null, PLATFORM_KEY + ":1.0",
                catalogResolver, null, null);
        assertThat(catalog.getQuarkusCoreVersion()).isEqualTo("1.0.5");
    }

    @Test
    void streamWithPlatformCoordsThrows() {
        assertThatThrownBy(() -> CreateProjectMojo.resolveExtensionsCatalog(
                dummyMojo, "io.quarkus", null, null, "1.0",
                catalogResolver, null, null))
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("cannot be combined with");
    }

    @ParameterizedTest
    @ValueSource(strings = { "3.15", "io.quarkus.platform:3.15", " 3.15 " })
    void streamWithNoRegistriesThrows(String stream) {
        assertThatThrownBy(() -> CreateProjectMojo.resolveExtensionsCatalog(
                dummyMojo, null, null, null, stream,
                ExtensionCatalogResolver.empty(), null, null))
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("Specifying a stream requires the Quarkus extension registry client");
    }

    @Test
    void nullStreamSkipsStreamResolution() {
        assertThatThrownBy(() -> CreateProjectMojo.resolveExtensionsCatalog(
                dummyMojo, null, null, null, null,
                ExtensionCatalogResolver.empty(), null, null))
                .isNotInstanceOf(MojoExecutionException.class);
    }
}
