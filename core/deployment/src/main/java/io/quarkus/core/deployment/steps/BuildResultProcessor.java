package io.quarkus.core.deployment.steps;

import io.quarkus.core.deployment.builditem.AlwaysResultBuildItem;
import io.quarkus.core.deployment.builditem.DevResultBuildItem;
import io.quarkus.core.deployment.builditem.ProductionResultBuildItem;
import io.quarkus.core.deployment.builditem.TestResultBuildItem;
import io.quarkus.core.deployment.service.impl.ServiceMetadataBuildItem;
import io.quarkus.core.deployment.service.impl.StaticServiceMetadataBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.ApplicationClassNameBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedFileSystemResourceHandledBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.GeneratedServiceProviderBuildItem;
import io.quarkus.deployment.builditem.MainClassBuildItem;
import io.quarkus.deployment.builditem.RuntimeApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.RuntimeClassTransformerBuildItem;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem;
import io.quarkus.deployment.jvm.ResolvedJVMRequirements;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.DeploymentResultBuildItem;
import io.quarkus.deployment.sbom.SbomBuildItem;

/**
 * Bridge steps that connect build result items to the mode-specific result build items.
 * <p>
 * The mode-specific result items ({@link ProductionResultBuildItem}, {@link DevResultBuildItem},
 * {@link TestResultBuildItem}) are declared as final build items in their respective build modes.
 * The bridge steps in this processor establish dependency edges from the items that were previously
 * declared as finals to the new mode-specific result items, so that those items and their transitive
 * producers are included in the build chain.
 */
public class BuildResultProcessor {

    // Always → mode-specific bridges

    /**
     * Bridge step that connects {@link AlwaysResultBuildItem} producers into production builds.
     */
    @BuildStep
    @Consume(AlwaysResultBuildItem.class)
    @Produce(ProductionResultBuildItem.class)
    void alwaysToProductionBridge() {
    }

    /**
     * Bridge step that connects {@link AlwaysResultBuildItem} producers into dev-mode builds.
     */
    @BuildStep
    @Consume(AlwaysResultBuildItem.class)
    @Produce(DevResultBuildItem.class)
    void alwaysToDevBridge() {
    }

    /**
     * Bridge step that connects {@link AlwaysResultBuildItem} producers into test builds.
     */
    @BuildStep
    @Consume(AlwaysResultBuildItem.class)
    @Produce(TestResultBuildItem.class)
    void alwaysToTestBridge() {
    }

    // Service metadata → always bridge

    /**
     * Bridge step that pulls service metadata items into all build modes.
     */
    @BuildStep
    @Consume(ServiceMetadataBuildItem.class)
    @Consume(StaticServiceMetadataBuildItem.class)
    @Produce(AlwaysResultBuildItem.class)
    void serviceMetadataBridge() {
    }

    // Production bridges

    /**
     * Bridge step that pulls production artifact, deployment, and SBOM results into production builds.
     */
    @BuildStep
    @Consume(ArtifactResultBuildItem.class)
    @Consume(DeploymentResultBuildItem.class)
    @Consume(SbomBuildItem.class)
    @Produce(ProductionResultBuildItem.class)
    void productionArtifactBridge() {
    }

    // Dev/test bridges

    /**
     * Bridge step that pulls dev/test runtime items into dev-mode and test builds.
     * These items were previously declared as direct finals in non-normal launch modes.
     */
    @BuildStep
    @Consume(GeneratedClassBuildItem.class)
    @Consume(GeneratedResourceBuildItem.class)
    @Consume(GeneratedServiceProviderBuildItem.class)
    @Consume(ApplicationClassNameBuildItem.class)
    @Consume(MainClassBuildItem.class)
    @Consume(GeneratedFileSystemResourceHandledBuildItem.class)
    @Consume(TransformedClassesBuildItem.class)
    @Consume(RuntimeClassTransformerBuildItem.class)
    @Consume(ResolvedJVMRequirements.class)
    @Consume(RuntimeApplicationShutdownBuildItem.class)
    @Produce(DevResultBuildItem.class)
    @Produce(TestResultBuildItem.class)
    void devTestRuntimeBridge() {
    }
}
