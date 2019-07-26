package io.quarkus.creator.phase.runnerjar.test;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.resolver.ResolverSetupCleanup;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.creator.CuratedApplicationCreator;

public abstract class CreatorOutcomeTestBase extends ResolverSetupCleanup {

    protected TsArtifact appJar;

    @Test
    public void test() throws Exception {
        appJar = modelApp();
        appJar.install(repo);

        rebuild();
    }

    protected void rebuild() throws Exception {
        final CuratedApplicationCreator.Builder appCreationContext = CuratedApplicationCreator.builder()
                .setModelResolver(resolver)
                .setWorkDir(workDir)
                .setAppArtifact(resolver.resolve(appJar.toAppArtifact()));
        initProps(appCreationContext);
        testCreator(appCreationContext.build());

    }

    protected abstract TsArtifact modelApp() throws Exception;

    protected abstract void testCreator(CuratedApplicationCreator creator) throws Exception;

    protected void initProps(CuratedApplicationCreator.Builder builder) {
    }
}
