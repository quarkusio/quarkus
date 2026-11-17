package io.quarkus.deployment.pkg.builditem;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ResolvedArtifactDependency;
import io.quarkus.maven.dependency.ResolvedDependency;

class JarTreeShakeBuildItemTest {

    @TempDir
    Path depDir;

    @Test
    void unreachableMemberClassOfReachableOuterIsRemoved() throws Exception {
        ResolvedDependency dep = dependencyWith("lib/Outer.class", "lib/Outer$Inner.class");

        assertThat(unreachableEntries(dep, "lib.Outer")).containsExactly("lib/Outer$Inner.class");
    }

    @Test
    void reachableMemberClassIsKept() throws Exception {
        ResolvedDependency dep = dependencyWith("lib/Outer.class", "lib/Outer$Inner.class");

        assertThat(unreachableEntries(dep, "lib.Outer", "lib.Outer$Inner")).isEmpty();
    }

    @Test
    void unreachableTopLevelClassWithDollarInNameIsRemoved() throws Exception {
        ResolvedDependency dep = dependencyWith("lib/Weird.class", "lib/Weird$Name.class");

        assertThat(unreachableEntries(dep, "lib.Weird")).containsExactly("lib/Weird$Name.class");
    }

    @Test
    void multiReleaseEntryOfUnreachableClassIsRemoved() throws Exception {
        ResolvedDependency dep = dependencyWith("lib/Used.class", "lib/Unused.class",
                "META-INF/versions/17/lib/Unused.class");

        assertThat(unreachableEntries(dep, "lib.Used"))
                .containsExactlyInAnyOrder("lib/Unused.class", "META-INF/versions/17/lib/Unused.class");
    }

    @Test
    void nothingIsRemovedWhenClassesWereNotShaken() throws Exception {
        ResolvedDependency dep = dependencyWith("lib/Outer.class", "lib/Outer$Inner.class");
        JarTreeShakeBuildItem item = new JarTreeShakeBuildItem(false, Set.of(), Map.of(), Set.of());

        Set<String> removed = new HashSet<>();
        item.collectUnreachableEntries(dep, removed);

        assertThat(removed).isEmpty();
    }

    private Set<String> unreachableEntries(ResolvedDependency dep, String... reachableClassNames) throws IOException {
        JarTreeShakeBuildItem item = new JarTreeShakeBuildItem(true, Set.of(reachableClassNames), Map.of(), Set.of());
        Set<String> removed = new HashSet<>();
        item.collectUnreachableEntries(dep, removed);
        return removed;
    }

    private ResolvedDependency dependencyWith(String... entries) throws IOException {
        for (String entry : entries) {
            Path file = depDir.resolve(entry);
            Files.createDirectories(file.getParent());
            Files.write(file, List.of());
        }
        return new ResolvedArtifactDependency(ArtifactCoords.jar("org.acme", "lib", "1.0"), depDir);
    }
}
