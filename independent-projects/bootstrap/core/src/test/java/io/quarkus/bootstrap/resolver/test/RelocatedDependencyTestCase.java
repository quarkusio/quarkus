package io.quarkus.bootstrap.resolver.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.resolver.ResolverSetupCleanup;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsDependency;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;

/**
 * Tests that Maven relocations are properly handled in the dependency resolver.
 * <p>
 * When a dependency is relocated (e.g., CDI 5.0 moved from
 * {@code jakarta.enterprise:jakarta.enterprise.cdi-api} to {@code jakarta.cdi:jakarta.cdi-api}),
 * libraries that still reference the old coordinates should have their direct dependencies
 * resolved to the new coordinates rather than being flagged as {@code MISSING_FROM_APPLICATION}.
 */
public class RelocatedDependencyTestCase extends ResolverSetupCleanup {

    @Test
    public void testRelocatedDependencyIsResolved() throws Exception {
        // the real artifact under new coordinates
        TsArtifact newApi = TsArtifact.jar("new.group", "new-api", "1");
        install(newApi);

        // relocation POM under old coordinates pointing to new
        TsArtifact oldApi = TsArtifact.pom("old.group", "old-api", "1");
        oldApi.setRelocation(newApi);
        install(oldApi);

        // a library that depends on the old coordinates (provided scope)
        TsArtifact lib = TsArtifact.jar("lib", "1");
        lib.addDependency(new TsDependency(
                new TsArtifact("old.group", "old-api", ArtifactCoords.DEFAULT_CLASSIFIER, "jar", "1"),
                "provided"));
        install(lib);

        // app depends on the library and the new API directly
        TsArtifact app = TsArtifact.jar("app", "1");
        app.addDependency(new TsDependency(lib));
        app.addDependency(new TsDependency(newApi));

        // manage both old and new coordinates to the same version (like a BOM would)
        app.addManagedDependency(new TsDependency(
                new TsArtifact("old.group", "old-api", ArtifactCoords.DEFAULT_CLASSIFIER, "jar", "1")));
        app.addManagedDependency(new TsDependency(newApi));

        install(app);

        var model = resolver.resolveModel(app.toArtifact());
        var deps = model.getDependencies();

        // the new API should be among the resolved dependencies
        assertThat(deps).anySatisfy(d -> assertThat(d.getArtifactId()).isEqualTo("new-api"));
        // the old API should NOT be among the resolved dependencies (it's a POM-only relocation)
        assertThat(deps).noneSatisfy(d -> assertThat(d.getArtifactId()).isEqualTo("old-api"));

        for (ResolvedDependency dep : deps) {
            if ("lib".equals(dep.getArtifactId())) {
                // the library's direct dependency on the old coordinates should be resolved
                // to the new coordinates, NOT flagged as MISSING_FROM_APPLICATION
                assertThat(dep.getDirectDependencies()).anySatisfy(d -> {
                    assertThat(d.getGroupId()).isEqualTo("new.group");
                    assertThat(d.getArtifactId()).isEqualTo("new-api");
                    assertThat(d.isFlagSet(DependencyFlags.MISSING_FROM_APPLICATION)).isFalse();
                });
                assertThat(dep.getDirectDependencies()).noneSatisfy(d -> {
                    assertThat(d.getGroupId()).isEqualTo("old.group");
                });
                break;
            }
        }
    }
}
