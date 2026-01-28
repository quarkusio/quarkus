package io.quarkus.deployment.pkg.jar;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.runner.QuarkusEntryPoint;
import io.quarkus.bootstrap.runner.SerializedApplication;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveBuildItem;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.MainClassBuildItem;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem;
import io.quarkus.deployment.jvm.ResolvedJVMRequirements;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;

public class FastJarBuilder extends AbstractFastJarBuilder {

    private static final Logger LOG = Logger.getLogger(FastJarBuilder.class);

    public FastJarBuilder(CurateOutcomeBuildItem curateOutcome,
            OutputTargetBuildItem outputTarget,
            ApplicationInfoBuildItem applicationInfo,
            PackageConfig packageConfig,
            MainClassBuildItem mainClass,
            ApplicationArchivesBuildItem applicationArchives,
            List<AdditionalApplicationArchiveBuildItem> additionalApplicationArchives,
            TransformedClassesBuildItem transformedClasses,
            List<GeneratedClassBuildItem> generatedClasses,
            List<GeneratedResourceBuildItem> generatedResources,
            Set<ArtifactKey> parentFirstArtifactKeys,
            Set<ArtifactKey> removedArtifactKeys,
            ExecutorService executorService,
            ResolvedJVMRequirements jvmRequirements) {
        super(curateOutcome, outputTarget, applicationInfo, packageConfig, mainClass, applicationArchives,
                additionalApplicationArchives, transformedClasses, generatedClasses, generatedResources,
                parentFirstArtifactKeys, removedArtifactKeys, executorService, jvmRequirements);
    }

    @Override
    protected void writeSerializedApplication(OutputStream out, Path buildDir, List<Path> allJars, List<Path> sortedParentFirst)
            throws IOException {
        SerializedApplication.write(out, mainClass.getClassName(), buildDir, allJars, sortedParentFirst);
    }

    @Override
    protected Class<?> getEntryPoint() {
        return QuarkusEntryPoint.class;
    }

    @Override
    protected String getClassPath(FastJarJars fastJarJars) {
        return fastJarJars.getParentFirstClassPath();
    }
}
