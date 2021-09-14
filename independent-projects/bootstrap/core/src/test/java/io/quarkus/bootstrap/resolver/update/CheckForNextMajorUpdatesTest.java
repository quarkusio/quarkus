package io.quarkus.bootstrap.resolver.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import java.util.Arrays;

public class CheckForNextMajorUpdatesTest extends CreatorOutcomeTestBase {

    @Override
    protected void initProps(QuarkusBootstrap.Builder builder) {
        builder.setVersionUpdateNumber(VersionUpdateNumber.MAJOR)
                .setVersionUpdate(VersionUpdate.NEXT);
    }

    @Override
    protected TsArtifact modelApp() throws Exception {
        new TsQuarkusExt("ext1", "1.0.1").install(repo);
        new TsQuarkusExt("ext1", "1.1.0").install(repo);
        new TsQuarkusExt("ext1", "2.0.0").install(repo);
        new TsQuarkusExt("ext1", "2.0.1").install(repo);
        new TsQuarkusExt("ext1", "2.1.0").install(repo);
        new TsQuarkusExt("ext1", "3.0.0").install(repo);

        return TsArtifact.jar("app")
                .addDependency(new TsQuarkusExt("ext1", "1.0.0"))
                .addDependency(TsArtifact.jar("random"))
                .addDependency(new TsQuarkusExt("ext2", "1.0.0"));
    }

    @Override
    protected void testCreator(QuarkusBootstrap creator) throws Exception {
        final CuratedApplication outcome = creator.bootstrap();

        assertTrue(outcome.hasUpdatedDeps());

        assertEquals(Arrays.asList(new AppDependency[] {
                new AppDependency(TsArtifact.jar("ext1", "2.0.0").toAppArtifact(), "compile")
        }), outcome.getUpdatedDeps());

        final AppModel effectiveModel = outcome.getAppModel();
        assertEquals(Arrays.asList(new AppDependency[] {
                new AppDependency(TsArtifact.jar("ext1", "2.0.0").toAppArtifact(), "compile", AppDependency.DIRECT_FLAG,
                        AppDependency.RUNTIME_EXTENSION_ARTIFACT_FLAG, AppDependency.RUNTIME_CP_FLAG,
                        AppDependency.DEPLOYMENT_CP_FLAG),
                new AppDependency(TsArtifact.jar("random").toAppArtifact(), "compile", AppDependency.DIRECT_FLAG,
                        AppDependency.RUNTIME_CP_FLAG, AppDependency.DEPLOYMENT_CP_FLAG),
                new AppDependency(TsArtifact.jar("ext2", "1.0.0").toAppArtifact(), "compile", AppDependency.DIRECT_FLAG,
                        AppDependency.RUNTIME_EXTENSION_ARTIFACT_FLAG, AppDependency.RUNTIME_CP_FLAG,
                        AppDependency.DEPLOYMENT_CP_FLAG)
        }), effectiveModel.getUserDependencies());

        assertEquals(Arrays.asList(new AppDependency[] {
                new AppDependency(TsArtifact.jar("ext1-deployment", "2.0.0").toAppArtifact(), "compile",
                        AppDependency.DEPLOYMENT_CP_FLAG),
                new AppDependency(TsArtifact.jar("ext2-deployment", "1.0.0").toAppArtifact(), "compile",
                        AppDependency.DEPLOYMENT_CP_FLAG)
        }), effectiveModel.getDeploymentDependencies());

    }
}
