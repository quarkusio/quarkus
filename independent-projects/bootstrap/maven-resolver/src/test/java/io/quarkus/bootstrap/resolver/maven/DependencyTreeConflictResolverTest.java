package io.quarkus.bootstrap.resolver.maven;

import static io.quarkus.bootstrap.util.DependencyUtils.getKey;
import static io.quarkus.bootstrap.util.DependencyUtils.hasWinner;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.dependency.ArtifactKey;

public class DependencyTreeConflictResolverTest {

    private static final String ORG_ACME = "org.acme";
    private static final String JAR = "jar";
    private static final String VERSION = "1.0";

    @Test
    public void conditionalDependencyIsAddedToWinner() {
        var devLib = node("dev-lib");
        var extDev = node("ext-dev-deployment", devLib);
        var extA = node("ext-a", node("a-lib"));
        var extACopy = node("ext-a");
        var root = node("root", node("b", extA), node("d", extACopy));
        var conditionalDeps = conditionalDeps(extA, extDev);

        DependencyTreeConflictResolver.resolveWithConditionalDeps(root, conditionalDeps);

        assertThat(hasWinner(extACopy)).isTrue();
        assertThat(extA.getChildren()).extracting(n -> n.getArtifact().getArtifactId())
                .containsExactly("a-lib", "ext-dev-deployment");
        assertThat(hasWinner(extDev)).isFalse();
        assertThat(extDev.getChildren()).containsExactly(devLib);
        assertThat(hasWinner(devLib)).isFalse();
        assertThat(conditionalDeps).isEmpty();
    }

    @Test
    public void conditionalDependencyOfExtensionBelowLosingNodeIsAddedToWinner() {
        // ext-f, which declares the conditional dependency, is a dependency of ext-a that loses conflict resolution
        var extDev = node("ext-dev-deployment");
        var extF = node("ext-f");
        var extACopy = node("ext-a", node("ext-f"));
        var root = node("root", node("b", node("ext-a", extF)), node("d", extACopy));

        DependencyTreeConflictResolver.resolveWithConditionalDeps(root, conditionalDeps(extF, extDev));

        assertThat(hasWinner(extACopy)).isTrue();
        assertThat(extF.getChildren()).containsExactly(extDev);
        assertThat(hasWinner(extDev)).isFalse();
    }

    @Test
    public void conditionalDependencyTakesPartInConflictResolutionAtTheWinnerPosition() {
        var lib2 = node("lib", "2.0");
        var extDev = node("ext-dev-deployment", lib2);
        var extA = node("ext-a");
        var lib1 = node("lib", "1.0");
        var root = node("root",
                node("b", extA),
                node("c", node("x", node("y", node("z", lib1)))),
                node("d", node("ext-a")));

        DependencyTreeConflictResolver.resolveWithConditionalDeps(root, conditionalDeps(extA, extDev));

        // lib:2.0 is closer to the root as a dependency of ext-dev-deployment added to the winning ext-a
        assertThat(hasWinner(lib2)).isFalse();
        assertThat(hasWinner(lib1)).isTrue();
    }

    private static Map<ArtifactKey, List<DependencyNode>> conditionalDeps(DependencyNode extension,
            DependencyNode... conditionalDeps) {
        // the map has to be mutable, since the conflict resolver removes the entries it processed
        var result = new HashMap<ArtifactKey, List<DependencyNode>>(1);
        result.put(getKey(extension.getArtifact()), List.of(conditionalDeps));
        return result;
    }

    private static DependencyNode node(String artifactId, DependencyNode... children) {
        return node(artifactId, VERSION, children);
    }

    private static DependencyNode node(String artifactId, String version, DependencyNode... children) {
        var node = new DefaultDependencyNode(
                new Dependency(new DefaultArtifact(ORG_ACME, artifactId, JAR, version), JavaScopes.COMPILE));
        node.setChildren(new ArrayList<>(List.of(children)));
        return node;
    }
}
