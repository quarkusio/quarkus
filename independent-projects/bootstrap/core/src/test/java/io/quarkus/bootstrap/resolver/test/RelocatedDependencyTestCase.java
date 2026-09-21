package io.quarkus.bootstrap.resolver.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.resolver.ResolverSetupCleanup;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsDependency;
import io.quarkus.maven.dependency.Dependency;
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

    private static final String NEW_GROUP_ID = "new.group";

    private static final String OLD_GROUP_ID = "old.group";

    @Test
    public void testRelocatedDependencyWithDependencyManagementVersionEnforcement() throws Exception {
        // Test that dependencyManagement version is enforced on relocated artifacts
        // The relocation POM has version 1, but the app manages the NEW coordinates at version 3
        // (This is the realistic Maven scenario - BOMs manage the new coordinates, not the old ones)
        TsArtifact newApi = TsArtifact.jar(NEW_GROUP_ID, "new-api", "3");
        install(newApi);

        TsArtifact oldApi = TsArtifact.pom(OLD_GROUP_ID, "old-api", "1");
        oldApi.setRelocation(TsArtifact.jar(NEW_GROUP_ID, "next-api", "1"));
        install(oldApi);

        TsArtifact nextApi = TsArtifact.pom(NEW_GROUP_ID, "next-api", "2");
        nextApi.setRelocation(TsArtifact.jar(NEW_GROUP_ID, "new-api", "2"));
        install(nextApi);

        // Library depends on old-api version 1
        TsArtifact lib = TsArtifact.jar(NEW_GROUP_ID, "lib", "2");
        lib.addDependency(new TsDependency(TsArtifact.jar(OLD_GROUP_ID, "old-api", "1"), true));
        install(lib);

        // App manages version 3 for the NEW coordinates only (realistic BOM scenario)
        TsArtifact app = TsArtifact.jar("app", "1");
        app.addDependency(new TsDependency(lib));
        app.addManagedDependency(new TsDependency(TsArtifact.jar(NEW_GROUP_ID, "next-api", "2")));
        app.addManagedDependency(new TsDependency(newApi));

        install(app);

        var model = resolver.resolveModel(app.toArtifact());

        ResolvedDependency appArtifact = model.getAppArtifact();
        assertThat(appArtifact.getDirectDependencies()).hasSize(1);
        for (Dependency dep : appArtifact.getDirectDependencies()) {
            assertThat(dep.getGroupId()).isEqualTo(NEW_GROUP_ID);
            assertThat(dep.getArtifactId()).isEqualTo("lib");
            assertThat(dep.getVersion()).isEqualTo("2");
        }

        var deps = model.getDependencies();
        for (ResolvedDependency dep : deps) {
            if ("lib".equals(dep.getArtifactId())) {
                Collection<Dependency> directDeps = dep.getDirectDependencies();
                assertThat(directDeps).hasSize(1);
                Dependency dd = directDeps.iterator().next();
                assertThat(dd.getGroupId()).isEqualTo(NEW_GROUP_ID);
                assertThat(dd.getArtifactId()).isEqualTo("new-api");
                assertThat(dd.getVersion()).isEqualTo("3");
                assertThat(dd.isFlagSet(DependencyFlags.MISSING_FROM_APPLICATION)).isTrue();
            }
        }
    }

    @Test
    public void testRelocatedOptionalDependency() throws Exception {
        TsArtifact newApi = TsArtifact.jar(NEW_GROUP_ID, "new-api", "2");
        install(newApi);

        TsArtifact oldApi = TsArtifact.pom(OLD_GROUP_ID, "old-api", "1");
        oldApi.setRelocation(newApi);
        install(oldApi);

        // Library has optional dependency on old-api
        TsArtifact lib = TsArtifact.jar(NEW_GROUP_ID, "lib", "2");
        lib.addDependency(new TsDependency(TsArtifact.jar(OLD_GROUP_ID, "old-api", "1"), true));
        install(lib);

        TsArtifact app = TsArtifact.jar("app", "1");
        app.addDependency(new TsDependency(lib));
        app.addDependency(new TsDependency(newApi));

        install(app);

        var model = resolver.resolveModel(app.toArtifact());

        // Find lib and check its direct dependency
        for (ResolvedDependency dep : model.getDependencies()) {
            if ("lib".equals(dep.getArtifactId())) {
                Collection<Dependency> directDeps = dep.getDirectDependencies();
                assertThat(directDeps).hasSize(1);
                Dependency dd = directDeps.iterator().next();
                assertThat(dd.getGroupId()).isEqualTo(NEW_GROUP_ID);
                assertThat(dd.getArtifactId()).isEqualTo("new-api");
                assertThat(dd.isOptional()).isTrue();
                assertThat(dd.isFlagSet(DependencyFlags.MISSING_FROM_APPLICATION)).isFalse();
            }
        }
    }

    @Test
    public void testRelocatedProvidedDependency() throws Exception {
        TsArtifact newApi = TsArtifact.jar(NEW_GROUP_ID, "new-api", "2");
        install(newApi);

        TsArtifact oldApi = TsArtifact.pom(OLD_GROUP_ID, "old-api", "1");
        oldApi.setRelocation(newApi);
        install(oldApi);

        // Library has provided dependency on old-api
        TsArtifact lib = TsArtifact.jar(NEW_GROUP_ID, "lib", "2");
        lib.addDependency(new TsDependency(TsArtifact.jar(OLD_GROUP_ID, "old-api", "1"), "provided"));
        install(lib);

        TsArtifact app = TsArtifact.jar("app", "1");
        app.addDependency(new TsDependency(lib));
        app.addDependency(new TsDependency(newApi));

        install(app);

        var model = resolver.resolveModel(app.toArtifact());

        // Find lib and check its direct dependency
        for (ResolvedDependency dep : model.getDependencies()) {
            if ("lib".equals(dep.getArtifactId())) {
                Collection<Dependency> directDeps = dep.getDirectDependencies();
                assertThat(directDeps).hasSize(1);
                Dependency dd = directDeps.iterator().next();
                assertThat(dd.getGroupId()).isEqualTo(NEW_GROUP_ID);
                assertThat(dd.getArtifactId()).isEqualTo("new-api");
                assertThat(dd.getScope()).isEqualTo("provided");
                assertThat(dd.isFlagSet(DependencyFlags.MISSING_FROM_APPLICATION)).isFalse();
            }
        }
    }

    @Test
    public void testRelocatedRequiredDependency() throws Exception {
        TsArtifact newApi = TsArtifact.jar(NEW_GROUP_ID, "new-api", "2");
        install(newApi);

        TsArtifact oldApi = TsArtifact.pom(OLD_GROUP_ID, "old-api", "1");
        oldApi.setRelocation(newApi);
        install(oldApi);

        // Library has required (compile) dependency on old-api
        TsArtifact lib = TsArtifact.jar(NEW_GROUP_ID, "lib", "2");
        lib.addDependency(new TsDependency(TsArtifact.jar(OLD_GROUP_ID, "old-api", "1")));
        install(lib);

        TsArtifact app = TsArtifact.jar("app", "1");
        app.addDependency(new TsDependency(lib));
        app.addDependency(new TsDependency(newApi));

        install(app);

        var model = resolver.resolveModel(app.toArtifact());

        // Find lib and check its direct dependency
        for (ResolvedDependency dep : model.getDependencies()) {
            if ("lib".equals(dep.getArtifactId())) {
                Collection<Dependency> directDeps = dep.getDirectDependencies();
                assertThat(directDeps).hasSize(1);
                Dependency dd = directDeps.iterator().next();
                assertThat(dd.getGroupId()).isEqualTo(NEW_GROUP_ID);
                assertThat(dd.getArtifactId()).isEqualTo("new-api");
                assertThat(dd.getScope()).isEqualTo("compile");
                assertThat(dd.isFlagSet(DependencyFlags.MISSING_FROM_APPLICATION)).isFalse();
            }
        }
    }

    @Test
    public void testRelocationWithVersionProperty() throws Exception {
        TsArtifact newApi = TsArtifact.jar(NEW_GROUP_ID, "new-api", "2");
        install(newApi);

        TsArtifact oldApi = TsArtifact.pom(OLD_GROUP_ID, "old-api", "1");
        oldApi.setPomProperty("relocated.version", "2");
        oldApi.setRelocation(TsArtifact.jar(NEW_GROUP_ID, "new-api", "${relocated.version}"));
        install(oldApi);

        TsArtifact lib = TsArtifact.jar(NEW_GROUP_ID, "lib", "2");
        lib.addDependency(new TsDependency(TsArtifact.jar(OLD_GROUP_ID, "old-api", "1")));
        install(lib);

        TsArtifact app = TsArtifact.jar("app", "1");
        app.addDependency(new TsDependency(lib));
        app.addDependency(new TsDependency(newApi));

        install(app);

        var model = resolver.resolveModel(app.toArtifact());

        // Find lib and check its direct dependency
        for (ResolvedDependency dep : model.getDependencies()) {
            if ("lib".equals(dep.getArtifactId())) {
                Collection<Dependency> directDeps = dep.getDirectDependencies();
                assertThat(directDeps).hasSize(1);
                Dependency dd = directDeps.iterator().next();
                assertThat(dd.getGroupId()).isEqualTo(NEW_GROUP_ID);
                assertThat(dd.getArtifactId()).isEqualTo("new-api");
                assertThat(dd.getVersion()).isEqualTo("2");
                assertThat(dd.isFlagSet(DependencyFlags.MISSING_FROM_APPLICATION)).isFalse();
            }
        }
    }

    @Test
    public void testRelocationWithOnlyGroupId() throws Exception {
        TsArtifact newApi = TsArtifact.jar(NEW_GROUP_ID, "common-api", "1");
        install(newApi);

        TsArtifact oldApi = TsArtifact.pom(OLD_GROUP_ID, "common-api", "1");
        oldApi.setRelocation(NEW_GROUP_ID, null, null);
        install(oldApi);

        TsArtifact lib = TsArtifact.jar(NEW_GROUP_ID, "lib", "2");
        lib.addDependency(new TsDependency(TsArtifact.jar(OLD_GROUP_ID, "common-api", "1")));
        install(lib);

        TsArtifact app = TsArtifact.jar("app", "1");
        app.addDependency(new TsDependency(lib));
        app.addDependency(new TsDependency(newApi));

        install(app);

        var model = resolver.resolveModel(app.toArtifact());

        for (ResolvedDependency dep : model.getDependencies()) {
            if ("lib".equals(dep.getArtifactId())) {
                Collection<Dependency> directDeps = dep.getDirectDependencies();
                assertThat(directDeps).hasSize(1);
                Dependency dd = directDeps.iterator().next();
                assertThat(dd.getGroupId()).isEqualTo(NEW_GROUP_ID);
                assertThat(dd.getArtifactId()).isEqualTo("common-api");
                assertThat(dd.getVersion()).isEqualTo("1");
                assertThat(dd.isFlagSet(DependencyFlags.MISSING_FROM_APPLICATION)).isFalse();
            }
        }
    }

    @Test
    public void testRelocationWithOnlyArtifactId() throws Exception {
        TsArtifact newApi = TsArtifact.jar(OLD_GROUP_ID, "renamed-api", "1");
        install(newApi);

        TsArtifact oldApi = TsArtifact.pom(OLD_GROUP_ID, "old-name-api", "1");
        oldApi.setRelocation(null, "renamed-api", null);
        install(oldApi);

        TsArtifact lib = TsArtifact.jar(OLD_GROUP_ID, "lib", "2");
        lib.addDependency(new TsDependency(TsArtifact.jar(OLD_GROUP_ID, "old-name-api", "1")));
        install(lib);

        TsArtifact app = TsArtifact.jar("app", "1");
        app.addDependency(new TsDependency(lib));
        app.addDependency(new TsDependency(newApi));

        install(app);

        var model = resolver.resolveModel(app.toArtifact());

        for (ResolvedDependency dep : model.getDependencies()) {
            if ("lib".equals(dep.getArtifactId())) {
                Collection<Dependency> directDeps = dep.getDirectDependencies();
                assertThat(directDeps).hasSize(1);
                Dependency dd = directDeps.iterator().next();
                assertThat(dd.getGroupId()).isEqualTo(OLD_GROUP_ID);
                assertThat(dd.getArtifactId()).isEqualTo("renamed-api");
                assertThat(dd.getVersion()).isEqualTo("1");
                assertThat(dd.isFlagSet(DependencyFlags.MISSING_FROM_APPLICATION)).isFalse();
            }
        }
    }

    @Test
    public void testRelocationWithoutVersion() throws Exception {
        TsArtifact newApi = TsArtifact.jar(NEW_GROUP_ID, "new-api", "1");
        install(newApi);

        TsArtifact oldApi = TsArtifact.pom(OLD_GROUP_ID, "old-api", "1");
        oldApi.setRelocation(NEW_GROUP_ID, "new-api", null);
        install(oldApi);

        TsArtifact lib = TsArtifact.jar(NEW_GROUP_ID, "lib", "2");
        lib.addDependency(new TsDependency(TsArtifact.jar(OLD_GROUP_ID, "old-api", "1")));
        install(lib);

        TsArtifact app = TsArtifact.jar("app", "1");
        app.addDependency(new TsDependency(lib));
        app.addDependency(new TsDependency(newApi));

        install(app);

        var model = resolver.resolveModel(app.toArtifact());

        for (ResolvedDependency dep : model.getDependencies()) {
            if ("lib".equals(dep.getArtifactId())) {
                Collection<Dependency> directDeps = dep.getDirectDependencies();
                assertThat(directDeps).hasSize(1);
                Dependency dd = directDeps.iterator().next();
                assertThat(dd.getGroupId()).isEqualTo(NEW_GROUP_ID);
                assertThat(dd.getArtifactId()).isEqualTo("new-api");
                assertThat(dd.getVersion()).isEqualTo("1");
                assertThat(dd.isFlagSet(DependencyFlags.MISSING_FROM_APPLICATION)).isFalse();
            }
        }
    }
}
