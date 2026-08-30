package io.quarkus.gradle.application.internal.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.junit.jupiter.api.Test;

class ApplicationModelAndCodegenRegistrationTest {

    @Test
    void collectsRuntimeAndDeploymentGraphsWithTheSameRootComponent() {
        ComponentIdentifier rootId = mock(ComponentIdentifier.class);
        ResolvedComponentResult runtimeRoot = component(rootId);
        ResolvedComponentResult deploymentRoot = component(rootId);
        ResolvedComponentResult runtime = module("org.acme", "runtime", "1.0");
        ResolvedComponentResult deployment = module("org.acme", "deployment", "1.0");
        doReturn(Set.of(dependency(runtime))).when(runtimeRoot).getDependencies();
        doReturn(Set.of(dependency(deployment))).when(deploymentRoot).getDependencies();

        assertThat(ApplicationModelTaskRegistration.externalModuleGavs(runtimeRoot, deploymentRoot))
                .containsExactly("org.acme:deployment:1.0", "org.acme:runtime:1.0");
    }

    private static ResolvedComponentResult module(String groupId, String artifactId, String version) {
        ModuleComponentIdentifier id = mock(ModuleComponentIdentifier.class);
        when(id.getGroup()).thenReturn(groupId);
        when(id.getModule()).thenReturn(artifactId);
        when(id.getVersion()).thenReturn(version);
        return component(id);
    }

    private static ResolvedComponentResult component(ComponentIdentifier id) {
        ResolvedComponentResult component = mock(ResolvedComponentResult.class);
        when(component.getId()).thenReturn(id);
        when(component.getDependencies()).thenReturn(Set.of());
        return component;
    }

    private static ResolvedDependencyResult dependency(ResolvedComponentResult selected) {
        ResolvedDependencyResult dependency = mock(ResolvedDependencyResult.class);
        when(dependency.getSelected()).thenReturn(selected);
        return dependency;
    }
}
