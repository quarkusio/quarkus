package io.quarkus.bootstrap.resolver.update;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.resolver.ResolverSetupCleanup;
import io.quarkus.bootstrap.resolver.TsArtifact;

public abstract class CreatorOutcomeTestBase extends ResolverSetupCleanup {

    protected TsArtifact appJar;

    @Test
    public void test() throws Exception {
        appJar = modelApp();
        appJar.install(repo);

        rebuild();
    }

    protected void rebuild() throws Exception {
        final QuarkusBootstrap.Builder appCreationContext = QuarkusBootstrap.builder(resolver.resolve(appJar.toAppArtifact()))
                .setTargetDirectory(workDir)
                .setAppModelResolver(resolver);
        initProps(appCreationContext);
        testCreator(appCreationContext.build());

    }

    protected abstract TsArtifact modelApp() throws Exception;

    protected abstract void testCreator(QuarkusBootstrap creator) throws Exception;

    protected void initProps(QuarkusBootstrap.Builder builder) {
    }
}
