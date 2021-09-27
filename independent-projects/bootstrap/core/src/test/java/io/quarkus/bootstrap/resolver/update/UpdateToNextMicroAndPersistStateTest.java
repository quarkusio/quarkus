package io.quarkus.bootstrap.resolver.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import java.util.Arrays;
import java.util.Collections;

public class UpdateToNextMicroAndPersistStateTest extends CreatorOutcomeTestBase {

    private static final String[] EXPECTED_UPDATES = new String[] { "1.0.1", "1.0.2" };

    private int buildNo;

    @Override
    protected void initProps(QuarkusBootstrap.Builder builder) {
        builder.setVersionUpdateNumber(VersionUpdateNumber.MICRO)
                .setVersionUpdate(VersionUpdate.NEXT)
                .setDependenciesOrigin(DependenciesOrigin.LAST_UPDATE);
    }

    @Override
    protected TsArtifact modelApp() throws Exception {
        new TsQuarkusExt("ext1", "1.0.1").install(repo);
        new TsQuarkusExt("ext1", "1.0.2").install(repo);
        new TsQuarkusExt("ext1", "1.1.0").install(repo);
        new TsQuarkusExt("ext1", "2.0.0").install(repo);

        return TsArtifact.jar("app")
                .addDependency(new TsQuarkusExt("ext1", "1.0.0"))
                .addDependency(TsArtifact.jar("random"))
                .addDependency(new TsQuarkusExt("ext2", "1.0.0"));
    }

    @Override
    protected void testCreator(QuarkusBootstrap creator) throws Exception {

        final CuratedApplication outcome = creator.bootstrap();

        final String expectedVersion;

        if (buildNo < EXPECTED_UPDATES.length) {
            expectedVersion = EXPECTED_UPDATES[buildNo];
            assertTrue(outcome.hasUpdatedDeps());
            assertEquals(Arrays.asList(new AppDependency[] {
                    new AppDependency(TsArtifact.jar("ext1", expectedVersion).toAppArtifact(), "compile")
            }), outcome.getUpdatedDeps());
        } else {
            expectedVersion = EXPECTED_UPDATES[buildNo - 1];
            assertFalse(outcome.hasUpdatedDeps());
            assertEquals(Collections.emptyList(), outcome.getUpdatedDeps());
        }

        final AppModel effectiveModel = outcome.getAppModel();
        assertEquals(Arrays.asList(new AppDependency[] {
                new AppDependency(TsArtifact.jar("ext1", expectedVersion).toAppArtifact(), "compile", AppDependency.DIRECT_FLAG,
                        AppDependency.RUNTIME_EXTENSION_ARTIFACT_FLAG, AppDependency.RUNTIME_CP_FLAG,
                        AppDependency.DEPLOYMENT_CP_FLAG),
                new AppDependency(TsArtifact.jar("random").toAppArtifact(), "compile", AppDependency.DIRECT_FLAG,
                        AppDependency.RUNTIME_CP_FLAG, AppDependency.DEPLOYMENT_CP_FLAG),
                new AppDependency(TsArtifact.jar("ext2", "1.0.0").toAppArtifact(), "compile", AppDependency.DIRECT_FLAG,
                        AppDependency.RUNTIME_EXTENSION_ARTIFACT_FLAG, AppDependency.RUNTIME_CP_FLAG,
                        AppDependency.DEPLOYMENT_CP_FLAG)
        }), effectiveModel.getUserDependencies());

        assertEquals(Arrays.asList(new AppDependency[] {
                new AppDependency(TsArtifact.jar("ext1-deployment", expectedVersion).toAppArtifact(), "compile",
                        AppDependency.DEPLOYMENT_CP_FLAG),
                new AppDependency(TsArtifact.jar("ext2-deployment", "1.0.0").toAppArtifact(), "compile",
                        AppDependency.DEPLOYMENT_CP_FLAG)
        }), effectiveModel.getDeploymentDependencies());

        outcome.getCurationResult().persist(outcome.getQuarkusBootstrap().getAppModelResolver());

        if (++buildNo <= EXPECTED_UPDATES.length) {
            rebuild();
        }
    }
}
