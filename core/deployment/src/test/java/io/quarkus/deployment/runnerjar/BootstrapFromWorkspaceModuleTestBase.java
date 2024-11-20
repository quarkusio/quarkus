package io.quarkus.deployment.runnerjar;

import java.nio.file.Path;

import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.workspace.WorkspaceModule;

public abstract class BootstrapFromWorkspaceModuleTestBase extends PackageAppTestBase {

    protected abstract WorkspaceModule composeApplication() throws Exception;

    protected QuarkusBootstrap.Builder initBootstrapBuilder()
            throws Exception {
        final ApplicationModel appModel = resolver.resolveModel(composeApplication());
        Path applicationRoot = appModel.getAppArtifact().getResolvedPaths().getSinglePath();
        final QuarkusBootstrap.Builder bootstrap = QuarkusBootstrap.builder()
                .setExistingModel(appModel)
                .setApplicationRoot(applicationRoot)
                .setProjectRoot(applicationRoot)
                .setTargetDirectory(appModel.getAppArtifact().getWorkspaceModule().getBuildDir().toPath())
                .setAppModelResolver(resolver);
        switch (getBootstrapMode()) {
            case PROD:
                break;
            case TEST:
                bootstrap.setTest(true);
                break;
            default:
                throw new IllegalArgumentException("Not supported bootstrap mode " + getBootstrapMode());
        }
        return bootstrap;
    }
}
