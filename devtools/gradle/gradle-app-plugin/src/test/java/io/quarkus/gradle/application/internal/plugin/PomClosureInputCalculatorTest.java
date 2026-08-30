package io.quarkus.gradle.application.internal.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.gradle.model.tasks.GradlePomClosureResolver;
import io.quarkus.maven.dependency.GAV;

class PomClosureInputCalculatorTest {

    @TempDir
    Path testDirectory;

    @Test
    void scansEachResolvedPomOnlyOnceAsClosureExpands() throws IOException {
        GAV initialGav = new GAV("org.acme", "initial", "1");
        Path initialPom = testDirectory.resolve("initial.pom");
        writePom(initialPom, initialGav, "initial.property", null);

        Path repository = testDirectory.resolve("repository");
        var pomResolver = GradlePomClosureResolver.withGradleArtifactResolution(
                Map.of(initialGav, initialPom.toFile()), null, List.of(repository.toFile()));
        Map<String, String> propertyValues = Map.of(
                "initial.property", "initial-value",
                "expanded.property", "expanded-value",
                "expanded.activation", "active");
        Map<String, Provider<String>> propertyProviders = new HashMap<>();
        ProviderFactory providers = providerFactory(propertyValues, propertyProviders);
        var referencedProperties = new PomClosureInputCalculator.ReferencedSystemProperties(
                providers, pomResolver);

        assertThat(referencedProperties.get()).containsExactlyInAnyOrderEntriesOf(
                Map.of("initial.property", "initial-value"));

        Files.writeString(initialPom, "not valid XML", StandardCharsets.UTF_8);
        GAV expandedGav = new GAV("org.acme", "expanded", "1");
        Path expandedPom = repository.resolve("org/acme/expanded/1/expanded-1.pom");
        writePom(expandedPom, expandedGav, "expanded.property", "!expanded.activation");
        pomResolver.prefetchPoms(List.of(expandedGav));

        assertThat(referencedProperties.get()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "initial.property", "initial-value",
                "expanded.property", "expanded-value",
                "expanded.activation", "active"));
        assertThat(referencedProperties.get()).containsExactlyInAnyOrderEntriesOf(propertyValues);

        propertyValues.keySet().forEach(name -> {
            verify(providers).systemProperty(name);
            verify(propertyProviders.get(name)).getOrNull();
        });
    }

    @Test
    void resolvesAnAbsentPropertyOnlyOnce() throws IOException {
        GAV gav = new GAV("org.acme", "absent", "1");
        Path pom = testDirectory.resolve("absent.pom");
        writePom(pom, gav, "absent.property", null);

        var pomResolver = GradlePomClosureResolver.withGradleArtifactResolution(
                Map.of(gav, pom.toFile()), null, List.of(testDirectory.resolve("repository").toFile()));
        ProviderFactory providers = mock(ProviderFactory.class);
        @SuppressWarnings("unchecked")
        Provider<String> provider = mock(Provider.class);
        when(providers.systemProperty("absent.property")).thenReturn(provider);
        var referencedProperties = new PomClosureInputCalculator.ReferencedSystemProperties(providers, pomResolver);

        assertThat(referencedProperties.get()).isEmpty();
        assertThat(referencedProperties.get()).isEmpty();

        verify(providers).systemProperty("absent.property");
        verify(provider).getOrNull();
    }

    private static ProviderFactory providerFactory(Map<String, String> values,
            Map<String, Provider<String>> propertyProviders) {
        ProviderFactory providers = mock(ProviderFactory.class);
        values.forEach((name, value) -> {
            @SuppressWarnings("unchecked")
            Provider<String> provider = mock(Provider.class);
            when(provider.getOrNull()).thenReturn(value);
            when(providers.systemProperty(name)).thenReturn(provider);
            propertyProviders.put(name, provider);
        });
        return providers;
    }

    private static void writePom(Path path, GAV gav, String propertyExpression, String activationProperty)
            throws IOException {
        Files.createDirectories(path.getParent());
        String profile = activationProperty == null ? "" : """
                <profiles>
                  <profile>
                    <id>activated</id>
                    <activation>
                      <property>
                        <name>%s</name>
                      </property>
                    </activation>
                  </profile>
                </profiles>
                """.formatted(activationProperty);
        Files.writeString(path, """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>%s</groupId>
                  <artifactId>%s</artifactId>
                  <version>%s</version>
                  <name>${%s}</name>
                  %s
                </project>
                """.formatted(gav.getGroupId(), gav.getArtifactId(), gav.getVersion(), propertyExpression, profile),
                StandardCharsets.UTF_8);
    }
}
