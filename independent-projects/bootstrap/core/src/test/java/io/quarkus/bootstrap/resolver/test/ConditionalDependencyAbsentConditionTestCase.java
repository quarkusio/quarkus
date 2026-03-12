package io.quarkus.bootstrap.resolver.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.resolver.ResolverSetupCleanup;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.maven.dependency.ResolvedDependency;

/**
 * Tests dependencyConditionAbsent behavior for both exact names and wildcard patterns.
 * <p>
 * ext-a offers ext-b as a conditional dependency.
 * ext-b declares an absence condition that must not be matched by any runtime artifact
 * for ext-b to be activated.
 */
public class ConditionalDependencyAbsentConditionTestCase extends ResolverSetupCleanup {

    /**
     * ext-b is activated because ext-c is not present in the dependency graph
     * and the exact absence condition {@code io.quarkus.bootstrap.test:ext-c} is satisfied.
     */
    @Test
    public void exactConditionActivatedWhenDependencyAbsent() throws Exception {
        final TsArtifact root = new TsArtifact("root");

        final TsQuarkusExt extB = new TsQuarkusExt("ext-b");
        extB.setDependencyConditionAbsent(TsArtifact.DEFAULT_GROUP_ID + ":ext-c");
        extB.install(repo);

        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");
        extA.setConditionalDeps(extB);
        extA.install(repo);
        root.addDependency(extA);

        repo.install(root);

        final Collection<ResolvedDependency> buildDeps = resolver.resolveModel(root.toArtifact()).getDependencies();
        assertThat(buildDeps).extracting(ResolvedDependency::getArtifactId).contains("ext-b");
    }

    /**
     * ext-b is NOT activated because ext-c is present in the dependency graph,
     * matching the exact absence condition {@code io.quarkus.bootstrap.test:ext-c}.
     */
    @Test
    public void exactConditionNotActivatedWhenDependencyPresent() throws Exception {
        final TsArtifact root = new TsArtifact("root");

        final TsQuarkusExt extB = new TsQuarkusExt("ext-b");
        extB.setDependencyConditionAbsent(TsArtifact.DEFAULT_GROUP_ID + ":ext-c");
        extB.install(repo);

        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");
        extA.setConditionalDeps(extB);
        extA.install(repo);
        root.addDependency(extA);

        final TsQuarkusExt extC = new TsQuarkusExt("ext-c");
        extC.install(repo);
        root.addDependency(extC);

        repo.install(root);

        final Collection<ResolvedDependency> buildDeps = resolver.resolveModel(root.toArtifact()).getDependencies();
        assertThat(buildDeps).extracting(ResolvedDependency::getArtifactId).doesNotContain("ext-b");
    }

    /**
     * ext-b is activated because no artifact matching the wildcard pattern {@code *:ext-c*} is present.
     */
    @Test
    public void wildcardConditionActivatedWhenDependencyAbsent() throws Exception {
        final TsArtifact root = new TsArtifact("root");

        final TsQuarkusExt extB = new TsQuarkusExt("ext-b");
        extB.setDependencyConditionAbsent("*:ext-c*");
        extB.install(repo);

        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");
        extA.setConditionalDeps(extB);
        extA.install(repo);
        root.addDependency(extA);

        repo.install(root);

        final Collection<ResolvedDependency> buildDeps = resolver.resolveModel(root.toArtifact()).getDependencies();
        assertThat(buildDeps).extracting(ResolvedDependency::getArtifactId).contains("ext-b");
    }

    /**
     * ext-b is NOT activated because ext-c is present and matches the wildcard pattern {@code *:ext-c*}.
     */
    @Test
    public void wildcardConditionNotActivatedWhenDependencyPresent() throws Exception {
        final TsArtifact root = new TsArtifact("root");

        final TsQuarkusExt extB = new TsQuarkusExt("ext-b");
        extB.setDependencyConditionAbsent("*:ext-c*");
        extB.install(repo);

        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");
        extA.setConditionalDeps(extB);
        extA.install(repo);
        root.addDependency(extA);

        final TsQuarkusExt extC = new TsQuarkusExt("ext-c");
        extC.install(repo);
        root.addDependency(extC);

        repo.install(root);

        final Collection<ResolvedDependency> buildDeps = resolver.resolveModel(root.toArtifact()).getDependencies();
        assertThat(buildDeps).extracting(ResolvedDependency::getArtifactId).doesNotContain("ext-b");
    }
}
