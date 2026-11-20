package io.quarkus.bootstrap.resolver.maven;

import static io.quarkus.bootstrap.util.DependencyUtils.hasWinner;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.junit.jupiter.api.Test;

public class DependencyTreeConflictResolverTest {

    private static final String ORG_ACME = "org.acme";
    private static final String JAR = "jar";
    private static final String VERSION = "1.0";

    @Test
    public void conditionalDependencyOfLosingNodeIsMovedToWinner() {
        var root = newNode("root");
        var b = newNode("b");
        var d = newNode("d");
        root.setChildren(new ArrayList<>(List.of(b, d)));

        var extA = newNode("ext-a");
        b.setChildren(new ArrayList<>(List.of(extA)));

        var extACopy = newNode("ext-a");
        d.setChildren(new ArrayList<>(List.of(extACopy)));
        var extDev = newNode("ext-dev-deployment");
        DependencyTreeConflictResolver.markConditionalDependency(extDev);
        extACopy.setChildren(new ArrayList<>(List.of(extDev)));
        var devLib = newNode("dev-lib");
        extDev.setChildren(new ArrayList<>(List.of(devLib)));

        DependencyTreeConflictResolver.resolveConflicts(root);

        assertThat(hasWinner(extACopy)).isTrue();
        assertThat(extA.getChildren()).containsExactly(extDev);
        assertThat(hasWinner(extDev)).isFalse();
        assertThat(extDev.getChildren()).containsExactly(devLib);
    }

    @Test
    public void conditionalDependencyInChildrenSharedWithLosingNodeIsKept() {
        var root = newNode("root");
        var b = newNode("b");
        var d = newNode("d");
        root.setChildren(new ArrayList<>(List.of(b, d)));

        var extDev = newNode("ext-dev-deployment");
        DependencyTreeConflictResolver.markConditionalDependency(extDev);
        var devLib = newNode("dev-lib");
        extDev.setChildren(new ArrayList<>(List.of(devLib)));

        // a runtime extension node and its copy sharing the same list of children
        var extA = newNode("ext-a");
        extA.setChildren(new ArrayList<>(List.of(extDev)));
        b.setChildren(new ArrayList<>(List.of(extA)));
        var extACopy = new DefaultDependencyNode(extA);
        extACopy.setChildren(extA.getChildren());
        d.setChildren(new ArrayList<>(List.of(extACopy)));

        DependencyTreeConflictResolver.resolveConflicts(root);

        assertThat(hasWinner(extACopy)).isTrue();
        assertThat(extA.getChildren()).containsExactly(extDev);
        assertThat(hasWinner(extDev)).isFalse();
        assertThat(extDev.getChildren()).containsExactly(devLib);
    }

    private static DependencyNode newNode(String artifactId) {
        return new DefaultDependencyNode(
                new Dependency(new DefaultArtifact(ORG_ACME, artifactId, JAR, VERSION), JavaScopes.COMPILE));
    }
}
