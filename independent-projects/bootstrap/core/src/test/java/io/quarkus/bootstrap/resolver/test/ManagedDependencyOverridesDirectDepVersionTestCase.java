package io.quarkus.bootstrap.resolver.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.resolver.ResolverSetupCleanup;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsDependency;
import io.quarkus.maven.dependency.ResolvedDependency;

/**
 * When a managing project (platform BOM) specifies a version for a dependency,
 * that version should override the version declared as a direct dependency of
 * the application artifact.
 */
public class ManagedDependencyOverridesDirectDepVersionTestCase extends ResolverSetupCleanup {

    @Test
    public void testManagedVersionOverridesDirectDep() throws Exception {
        final TsArtifact libV1 = TsArtifact.jar("lib", "1");
        install(libV1);

        final TsArtifact libV2 = TsArtifact.jar("lib", "2");
        install(libV2);

        // app declares lib:1 as a direct dependency
        final TsArtifact app = TsArtifact.jar("app", "1");
        app.addDependency(new TsDependency(libV1));
        install(app);

        // managing project manages lib to version 2
        final TsArtifact managingProject = TsArtifact.jar("managing-project", "1");
        managingProject.addManagedDependency(new TsDependency(libV2));
        install(managingProject);

        final var model = resolver.resolveManagedModel(
                app.toArtifact(),
                List.of(),
                managingProject.toArtifact(),
                Set.of());
        final var resolved = model.getDependencies();
        assertThat(resolved).hasSize(1);
        final ResolvedDependency dep = resolved.iterator().next();
        assertThat(dep.getVersion()).isEqualTo("2");
    }
}
