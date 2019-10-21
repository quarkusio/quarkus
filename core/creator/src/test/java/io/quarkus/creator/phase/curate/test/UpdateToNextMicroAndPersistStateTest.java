package io.quarkus.creator.phase.curate.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.creator.CuratedApplicationCreator;
import io.quarkus.creator.DependenciesOrigin;
import io.quarkus.creator.VersionUpdate;
import io.quarkus.creator.VersionUpdateNumber;
import io.quarkus.creator.curator.CurateOutcome;
import io.quarkus.creator.phase.runnerjar.test.CreatorOutcomeTestBase;

public class UpdateToNextMicroAndPersistStateTest extends CreatorOutcomeTestBase {

    private static final String[] EXPECTED_UPDATES = new String[] { "1.0.1", "1.0.2" };

    private int buildNo;

    @Override
    protected void initProps(CuratedApplicationCreator.Builder builder) {
        builder.setUpdateNumber(VersionUpdateNumber.MICRO)
                .setUpdate(VersionUpdate.NEXT)
                .setDepsOrigin(DependenciesOrigin.LAST_UPDATE);
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
    protected void testCreator(CuratedApplicationCreator creator) throws Exception {

        final CurateOutcome outcome = creator.runTask(CurateOutcomeCuratedTask.INSTANCE);

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

        final AppModel effectiveModel = outcome.getEffectiveModel();
        assertEquals(Arrays.asList(new AppDependency[] {
                new AppDependency(TsArtifact.jar("ext1", expectedVersion).toAppArtifact(), "compile"),
                new AppDependency(TsArtifact.jar("random").toAppArtifact(), "compile"),
                new AppDependency(TsArtifact.jar("ext2", "1.0.0").toAppArtifact(), "compile")
        }), effectiveModel.getUserDependencies());

        assertEquals(Arrays.asList(new AppDependency[] {
                new AppDependency(TsArtifact.jar("ext1-deployment", expectedVersion).toAppArtifact(), "compile"),
                new AppDependency(TsArtifact.jar("ext2-deployment", "1.0.0").toAppArtifact(), "compile")
        }), effectiveModel.getDeploymentDependencies());

        outcome.persist(creator);

        if (++buildNo <= EXPECTED_UPDATES.length) {
            rebuild();
        }
    }
}
