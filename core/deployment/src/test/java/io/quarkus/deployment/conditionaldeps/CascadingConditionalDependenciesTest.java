package io.quarkus.deployment.conditionaldeps;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.deployment.runnerjar.BootstrapFromOriginalJarTestBase;
import io.quarkus.maven.dependency.ArtifactDependency;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.GACTV;

public class CascadingConditionalDependenciesTest extends BootstrapFromOriginalJarTestBase {

    @Override
    protected TsArtifact composeApplication() {

        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");
        final TsQuarkusExt extB = new TsQuarkusExt("ext-b");
        final TsQuarkusExt extC = new TsQuarkusExt("ext-c");
        final TsQuarkusExt extD = new TsQuarkusExt("ext-d");
        final TsQuarkusExt extE = new TsQuarkusExt("ext-e");
        final TsQuarkusExt extF = new TsQuarkusExt("ext-f");
        final TsQuarkusExt extG = new TsQuarkusExt("ext-g");
        final TsQuarkusExt extH = new TsQuarkusExt("ext-h");

        extA.setConditionalDeps(extB, extF);

        extB.setDependencyCondition(extC);
        extB.setConditionalDeps(extD);

        extD.setDependencyCondition(extE);

        extE.addDependency(extC);
        extE.setDependencyCondition(extA);
        extE.setDependencyCondition(extG);

        extF.setDependencyCondition(extD);

        extG.setDependencyCondition(extH);

        addToExpectedLib(extA.getRuntime());
        addToExpectedLib(extB.getRuntime());
        addToExpectedLib(extC.getRuntime());
        addToExpectedLib(extD.getRuntime());
        addToExpectedLib(extE.getRuntime());
        addToExpectedLib(extF.getRuntime());

        install(extA);
        install(extB);
        install(extC);
        install(extD);
        install(extE);
        install(extF);
        install(extG);
        install(extH);

        return TsArtifact.jar("app")
                .addManagedDependency(platformDescriptor())
                .addManagedDependency(platformProperties())
                .addDependency(extA)
                .addDependency(extE);
    }

    @Override
    protected void assertAppModel(ApplicationModel model) throws Exception {
        final Set<Dependency> expected = new HashSet<>();
        expected.add(new ArtifactDependency(
                new GACTV(TsArtifact.DEFAULT_GROUP_ID, "ext-c-deployment", TsArtifact.DEFAULT_VERSION), "compile",
                DependencyFlags.DEPLOYMENT_CP));
        expected.add(new ArtifactDependency(
                new GACTV(TsArtifact.DEFAULT_GROUP_ID, "ext-a-deployment", TsArtifact.DEFAULT_VERSION), "compile",
                DependencyFlags.DEPLOYMENT_CP));
        expected.add(new ArtifactDependency(
                new GACTV(TsArtifact.DEFAULT_GROUP_ID, "ext-b-deployment", TsArtifact.DEFAULT_VERSION), "runtime",
                DependencyFlags.DEPLOYMENT_CP));
        expected.add(new ArtifactDependency(
                new GACTV(TsArtifact.DEFAULT_GROUP_ID, "ext-d-deployment", TsArtifact.DEFAULT_VERSION), "runtime",
                DependencyFlags.DEPLOYMENT_CP));
        expected.add(new ArtifactDependency(
                new GACTV(TsArtifact.DEFAULT_GROUP_ID, "ext-e-deployment", TsArtifact.DEFAULT_VERSION), "compile",
                DependencyFlags.DEPLOYMENT_CP));
        expected.add(new ArtifactDependency(
                new GACTV(TsArtifact.DEFAULT_GROUP_ID, "ext-f-deployment", TsArtifact.DEFAULT_VERSION), "runtime",
                DependencyFlags.DEPLOYMENT_CP));
        assertEquals(expected, getDeploymentOnlyDeps(model));
    }
}
