package io.quarkus.bootstrap.resolver.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

import java.util.Collection;

import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsDependency;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;

/**
 *
 * @author Alexey Loubyansky
 */
public class OnlyDirectOptionalDepsAreCollectedTestCase extends CollectDependenciesBase {

    @Override
    protected void setupDependencies() {
        installAsDep(new TsDependency(new TsArtifact("required-dep-a"), "compile"), true);
        installAsDep(new TsDependency(new TsArtifact("required-dep-b"), "runtime"), true);

        final TsArtifact nonOptionalCommon = new TsArtifact("non-optional-common");
        install(nonOptionalCommon, "compile", true);

        installAsDep(
                new TsDependency(
                        new TsArtifact("optional-dep")
                                .addDependency(new TsDependency(new TsArtifact("common", "1"), true))
                                .addDependency(new TsDependency(nonOptionalCommon, false))
                                .addDependency(new TsDependency(new TsArtifact("other", "1"), true)),
                        true),
                true);

        TsArtifact common2 = install(new TsArtifact("common", "2"), true);
        installAsDep(new TsArtifact("required-dep-c")
                .addDependency(common2), true);
    }

    @Override
    protected void assertBuildDependencies(Collection<ResolvedDependency> buildDeps) {

        for (ResolvedDependency dep : buildDeps) {
            switch (dep.getArtifactId()) {
                case "required-dep-a":
                case "required-dep-b":
                    assertThat(dep.getFlags()).isEqualTo(
                            DependencyFlags.RUNTIME_CP |
                                    DependencyFlags.DEPLOYMENT_CP |
                                    DependencyFlags.DIRECT);
                    assertThat(dep.getDirectDependencies()).isEmpty();
                    break;
                case "required-dep-c":
                    assertThat(dep.getFlags()).isEqualTo(
                            DependencyFlags.RUNTIME_CP |
                                    DependencyFlags.DEPLOYMENT_CP |
                                    DependencyFlags.DIRECT);
                    assertThat(dep.getDirectDependencies()).containsExactlyInAnyOrder(
                            Dependency.withFlags(
                                    TsArtifact.DEFAULT_GROUP_ID, "common", ArtifactCoords.DEFAULT_CLASSIFIER, "txt", "2",
                                    DependencyFlags.DIRECT |
                                            DependencyFlags.RUNTIME_CP |
                                            DependencyFlags.DEPLOYMENT_CP));
                    break;
                case "optional-dep":
                    assertThat(dep.getFlags()).isEqualTo(
                            DependencyFlags.RUNTIME_CP |
                                    DependencyFlags.DEPLOYMENT_CP |
                                    DependencyFlags.DIRECT |
                                    DependencyFlags.OPTIONAL);
                    assertThat(dep.getDirectDependencies()).containsExactlyInAnyOrder(
                            Dependency.withFlags(TsArtifact.DEFAULT_GROUP_ID, "other", ArtifactCoords.DEFAULT_CLASSIFIER, "txt",
                                    "1",
                                    DependencyFlags.DIRECT |
                                            DependencyFlags.OPTIONAL |
                                            DependencyFlags.MISSING_FROM_APPLICATION),
                            Dependency.withFlags(TsArtifact.DEFAULT_GROUP_ID, "common", ArtifactCoords.DEFAULT_CLASSIFIER,
                                    "txt", "2",
                                    DependencyFlags.DIRECT |
                                            DependencyFlags.OPTIONAL |
                                            DependencyFlags.RUNTIME_CP |
                                            DependencyFlags.DEPLOYMENT_CP),
                            Dependency.withFlags(TsArtifact.DEFAULT_GROUP_ID, "non-optional-common",
                                    ArtifactCoords.DEFAULT_CLASSIFIER, "txt", "1",
                                    DependencyFlags.DIRECT |
                                            DependencyFlags.RUNTIME_CP |
                                            DependencyFlags.DEPLOYMENT_CP));
                    break;
                case "non-optional-common":
                    assertThat(dep.getFlags()).isEqualTo(
                            DependencyFlags.RUNTIME_CP |
                                    DependencyFlags.DEPLOYMENT_CP |
                                    DependencyFlags.OPTIONAL);
                    assertThat(dep.getDirectDependencies()).isEmpty();
                    break;
                case "common":
                    assertThat(dep.getFlags()).isEqualTo(
                            DependencyFlags.RUNTIME_CP |
                                    DependencyFlags.DEPLOYMENT_CP);
                    assertThat(dep.getDirectDependencies()).isEmpty();
                    break;
                default:
                    fail("Unexpected dependency: " + dep.toCompactCoords() + " " + DependencyFlags.toNames(dep.getFlags()));
            }
        }
    }
}
