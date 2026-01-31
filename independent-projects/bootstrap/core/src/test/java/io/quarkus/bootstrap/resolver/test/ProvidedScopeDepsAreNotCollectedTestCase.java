package io.quarkus.bootstrap.resolver.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

import java.util.Collection;

import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsDependency;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.DependencyBuilder;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;

/**
 *
 * @author Alexey Loubyansky
 */
public class ProvidedScopeDepsAreNotCollectedTestCase extends CollectDependenciesBase {

    @Override
    protected void setupDependencies() {

        final TsArtifact notCollected = new TsArtifact("not-collected");
        //install(notCollected, false);

        final TsArtifact common1 = new TsArtifact("common", "1")
                .addDependency(
                        new TsDependency(
                                notCollected, "provided"));
        install(common1);
        addCollectedDep(common1, DependencyFlags.COMPILE_ONLY);

        installAsDep(new TsArtifact("required-dep")
                .addDependency(common1),
                true);

        installAsDep(
                new TsDependency(
                        new TsArtifact("provided-dep")
                                .addDependency(
                                        new TsArtifact("common", "2")),
                        "provided"),
                false);
    }

    @Override
    protected void assertBuildDependencies(Collection<ResolvedDependency> buildDeps) {
        for (ResolvedDependency dep : buildDeps) {
            switch (dep.getArtifactId()) {
                case "required-dep":
                    assertThat(dep.getFlags()).isEqualTo(
                            DependencyFlags.DIRECT |
                                    DependencyFlags.RUNTIME_CP |
                                    DependencyFlags.DEPLOYMENT_CP);
                    assertThat(dep.getDirectDependencies()).containsExactlyInAnyOrder(
                            Dependency.withFlags(TsArtifact.DEFAULT_GROUP_ID, "common", ArtifactCoords.DEFAULT_CLASSIFIER,
                                    "txt", "1",
                                    DependencyFlags.DIRECT |
                                            DependencyFlags.RUNTIME_CP |
                                            DependencyFlags.DEPLOYMENT_CP));
                    break;
                case "provided-dep":
                    assertThat(dep.getFlags()).isEqualTo(
                            DependencyFlags.DIRECT |
                                    DependencyFlags.RUNTIME_CP |
                                    DependencyFlags.DEPLOYMENT_CP);
                    assertThat(dep.getDirectDependencies()).containsExactlyInAnyOrder(
                            Dependency.withFlags(TsArtifact.DEFAULT_GROUP_ID, "common", ArtifactCoords.DEFAULT_CLASSIFIER,
                                    "txt", "1",
                                    DependencyFlags.DIRECT |
                                            DependencyFlags.RUNTIME_CP |
                                            DependencyFlags.DEPLOYMENT_CP));
                    break;
                case "common":
                    assertThat(dep.getFlags()).isEqualTo(
                            DependencyFlags.COMPILE_ONLY |
                                    DependencyFlags.RUNTIME_CP |
                                    DependencyFlags.DEPLOYMENT_CP);
                    assertThat(dep.getDirectDependencies()).containsExactlyInAnyOrder(
                            DependencyBuilder.newInstance()
                                    .setGroupId(TsArtifact.DEFAULT_GROUP_ID)
                                    .setArtifactId("not-collected")
                                    .setType("txt")
                                    .setVersion("1")
                                    .setScope("provided")
                                    .setFlags(
                                            DependencyFlags.DIRECT |
                                                    DependencyFlags.MISSING_FROM_APPLICATION)
                                    .build());
                    break;
                default:
                    fail("Unexpected dependency " + dep.toCompactCoords());
            }
        }
    }
}
