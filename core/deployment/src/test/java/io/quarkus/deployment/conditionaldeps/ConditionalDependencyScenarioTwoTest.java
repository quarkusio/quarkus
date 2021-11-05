package io.quarkus.deployment.conditionaldeps;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.deployment.runnerjar.ExecutableOutputOutcomeTestBase;
import io.quarkus.maven.dependency.ArtifactDependency;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.GACTV;

public class ConditionalDependencyScenarioTwoTest extends ExecutableOutputOutcomeTestBase {

    @Override
    protected TsArtifact modelApp() {

        // dependencies
        // f -> g -> h?(i,j) -> k -> t
        // l -> j -> p?(o)
        // m -> n?(g) -> i -> o?(h)
        // m -> r?(i) -> s?(t) -> u

        final TsQuarkusExt extF = new TsQuarkusExt("ext-f");
        final TsQuarkusExt extG = new TsQuarkusExt("ext-g");
        final TsQuarkusExt extH = new TsQuarkusExt("ext-h");
        final TsQuarkusExt extI = new TsQuarkusExt("ext-i");
        final TsQuarkusExt extJ = new TsQuarkusExt("ext-j");
        final TsQuarkusExt extK = new TsQuarkusExt("ext-k");
        final TsQuarkusExt extL = new TsQuarkusExt("ext-l");
        final TsQuarkusExt extM = new TsQuarkusExt("ext-m");
        final TsQuarkusExt extN = new TsQuarkusExt("ext-n");
        final TsQuarkusExt extO = new TsQuarkusExt("ext-o");
        final TsQuarkusExt extP = new TsQuarkusExt("ext-p");
        final TsQuarkusExt extR = new TsQuarkusExt("ext-r");
        final TsQuarkusExt extS = new TsQuarkusExt("ext-s");
        final TsQuarkusExt extT = new TsQuarkusExt("ext-t");
        final TsQuarkusExt extU = new TsQuarkusExt("ext-u");

        extF.addDependency(extG);

        extG.setConditionalDeps(extH);

        extH.setDependencyCondition(extI, extJ);
        extH.addDependency(extK);

        extL.addDependency(extJ);

        extM.setConditionalDeps(extN, extR);

        extN.setDependencyCondition(extG);
        extN.addDependency(extI);

        extI.setConditionalDeps(extO);

        extO.setDependencyCondition(extH);

        extJ.setConditionalDeps(extP);

        extP.setDependencyCondition(extO);

        extR.setDependencyCondition(extI);
        extR.setConditionalDeps(extS);

        extK.addDependency(extT);

        extS.setDependencyCondition(extT);
        extS.addDependency(extU);

        addToExpectedLib(extF.getRuntime());
        addToExpectedLib(extG.getRuntime());
        addToExpectedLib(extH.getRuntime());
        addToExpectedLib(extI.getRuntime());
        addToExpectedLib(extJ.getRuntime());
        addToExpectedLib(extK.getRuntime());
        addToExpectedLib(extL.getRuntime());
        addToExpectedLib(extM.getRuntime());
        addToExpectedLib(extN.getRuntime());
        addToExpectedLib(extO.getRuntime());
        addToExpectedLib(extP.getRuntime());
        addToExpectedLib(extR.getRuntime());
        addToExpectedLib(extS.getRuntime());
        addToExpectedLib(extT.getRuntime());
        addToExpectedLib(extU.getRuntime());

        install(extF);
        install(extG);
        install(extH);
        install(extI);
        install(extJ);
        install(extK);
        install(extL);
        install(extM);
        install(extN);
        install(extO);
        install(extP);
        install(extR);
        install(extS);
        install(extT);
        install(extU);

        return TsArtifact.jar("app")
                .addManagedDependency(platformDescriptor())
                .addManagedDependency(platformProperties())
                .addDependency(extF)
                .addDependency(extL)
                .addDependency(extM);
    }

    @Override
    protected void assertAppModel(ApplicationModel appModel) throws Exception {
        final Set<Dependency> deploymentDeps = appModel.getDependencies().stream()
                .filter(d -> d.isDeploymentCp() && !d.isRuntimeCp()).map(d -> new ArtifactDependency(d))
                .collect(Collectors.toSet());
        final Set<Dependency> expected = new HashSet<>();
        expected.add(new ArtifactDependency(
                new GACTV(TsArtifact.DEFAULT_GROUP_ID, "ext-f-deployment", TsArtifact.DEFAULT_VERSION), "compile",
                DependencyFlags.DEPLOYMENT_CP));
        expected.add(new ArtifactDependency(
                new GACTV(TsArtifact.DEFAULT_GROUP_ID, "ext-g-deployment", TsArtifact.DEFAULT_VERSION), "compile",
                DependencyFlags.DEPLOYMENT_CP));
        expected.add(new ArtifactDependency(
                new GACTV(TsArtifact.DEFAULT_GROUP_ID, "ext-h-deployment", TsArtifact.DEFAULT_VERSION), "runtime",
                DependencyFlags.DEPLOYMENT_CP));
        expected.add(new ArtifactDependency(
                new GACTV(TsArtifact.DEFAULT_GROUP_ID, "ext-k-deployment", TsArtifact.DEFAULT_VERSION), "runtime",
                DependencyFlags.DEPLOYMENT_CP));
        expected.add(new ArtifactDependency(
                new GACTV(TsArtifact.DEFAULT_GROUP_ID, "ext-l-deployment", TsArtifact.DEFAULT_VERSION), "compile",
                DependencyFlags.DEPLOYMENT_CP));
        expected.add(new ArtifactDependency(
                new GACTV(TsArtifact.DEFAULT_GROUP_ID, "ext-j-deployment", TsArtifact.DEFAULT_VERSION), "compile",
                DependencyFlags.DEPLOYMENT_CP));
        expected.add(new ArtifactDependency(
                new GACTV(TsArtifact.DEFAULT_GROUP_ID, "ext-m-deployment", TsArtifact.DEFAULT_VERSION), "compile",
                DependencyFlags.DEPLOYMENT_CP));
        expected.add(new ArtifactDependency(
                new GACTV(TsArtifact.DEFAULT_GROUP_ID, "ext-n-deployment", TsArtifact.DEFAULT_VERSION), "runtime",
                DependencyFlags.DEPLOYMENT_CP));
        expected.add(new ArtifactDependency(
                new GACTV(TsArtifact.DEFAULT_GROUP_ID, "ext-i-deployment", TsArtifact.DEFAULT_VERSION), "runtime",
                DependencyFlags.DEPLOYMENT_CP));
        expected.add(new ArtifactDependency(
                new GACTV(TsArtifact.DEFAULT_GROUP_ID, "ext-o-deployment", TsArtifact.DEFAULT_VERSION), "runtime",
                DependencyFlags.DEPLOYMENT_CP));
        expected.add(new ArtifactDependency(
                new GACTV(TsArtifact.DEFAULT_GROUP_ID, "ext-p-deployment", TsArtifact.DEFAULT_VERSION), "runtime",
                DependencyFlags.DEPLOYMENT_CP));
        expected.add(new ArtifactDependency(
                new GACTV(TsArtifact.DEFAULT_GROUP_ID, "ext-r-deployment", TsArtifact.DEFAULT_VERSION), "runtime",
                DependencyFlags.DEPLOYMENT_CP));
        expected.add(new ArtifactDependency(
                new GACTV(TsArtifact.DEFAULT_GROUP_ID, "ext-s-deployment", TsArtifact.DEFAULT_VERSION), "runtime",
                DependencyFlags.DEPLOYMENT_CP));
        expected.add(new ArtifactDependency(
                new GACTV(TsArtifact.DEFAULT_GROUP_ID, "ext-t-deployment", TsArtifact.DEFAULT_VERSION), "compile",
                DependencyFlags.DEPLOYMENT_CP));
        expected.add(new ArtifactDependency(
                new GACTV(TsArtifact.DEFAULT_GROUP_ID, "ext-u-deployment", TsArtifact.DEFAULT_VERSION), "runtime",
                DependencyFlags.DEPLOYMENT_CP));
        assertEquals(expected, new HashSet<>(deploymentDeps));
    }
}
