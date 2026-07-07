package io.quarkus.gradle.application.internal.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.gradle.api.GradleException;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import io.quarkus.gradle.application.dsl.QuarkusApplicationBuild;

class QuarkusApplicationIntegrationTestBuildReferenceTest {

    @Test
    void acceptsPlainBuildName() {
        var reference = QuarkusApplicationIntegrationTestBuildReference.of("fastJar");

        assertThat(reference.displayName()).isEqualTo("fastJar");
        assertThat(reference.buildName()).isEqualTo("fastJar");
    }

    @Test
    void acceptsNamedDomainObjectProviderWithoutRealizingIt() {
        @SuppressWarnings("unchecked")
        NamedDomainObjectProvider<QuarkusApplicationBuild> provider = mock(NamedDomainObjectProvider.class);
        when(provider.getName()).thenReturn("native");

        var reference = QuarkusApplicationIntegrationTestBuildReference.of(provider);

        assertThat(reference.displayName()).isEqualTo("native");
        assertThat(reference.buildName()).isEqualTo("native");
    }

    @Test
    void acceptsProviderOfBuildName() {
        Project project = ProjectBuilder.builder().build();
        Provider<String> provider = project.provider(() -> "mutableJar");

        var reference = QuarkusApplicationIntegrationTestBuildReference.of(provider);

        assertThat(reference.displayName()).contains("provider");
        assertThat(reference.buildName()).isEqualTo("mutableJar");
    }

    @Test
    void rejectsUnsupportedNotationImmediately() {
        assertThatThrownBy(() -> QuarkusApplicationIntegrationTestBuildReference.of(123))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("forQuarkusIntegrationTests(...) accepts a build name")
                .hasMessageContaining(Integer.class.getName());
    }

    @Test
    void rejectsBlankBuildNameImmediately() {
        assertThatThrownBy(() -> QuarkusApplicationIntegrationTestBuildReference.of(" "))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("requires a non-empty Quarkus application build name");
    }

    @Test
    void rejectsProviderResolvingToUnsupportedNotation() {
        Project project = ProjectBuilder.builder().build();
        Provider<Integer> provider = project.provider(() -> 123);
        var reference = QuarkusApplicationIntegrationTestBuildReference.of(provider);

        assertThatThrownBy(reference::buildName)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("provider must produce a QuarkusApplicationBuild or build name")
                .hasMessageContaining(Integer.class.getName());
    }

    @Test
    void rejectsProviderResolvingToBlankName() {
        Project project = ProjectBuilder.builder().build();
        Provider<String> provider = project.provider(() -> "");
        var reference = QuarkusApplicationIntegrationTestBuildReference.of(provider);

        assertThatThrownBy(reference::buildName)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("resolved to an empty Quarkus application build name");
    }
}
