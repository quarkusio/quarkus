package io.quarkus.deployment.pkg.jar;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.deployment.ResolvedJVMRequirements;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.MainClassBuildItem;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;

public class LegacyThinJarBuilder extends AbstractLegacyThinJarBuilder<JarBuildItem> {

    private static final Logger LOG = Logger.getLogger(LegacyThinJarBuilder.class);

    public LegacyThinJarBuilder(CurateOutcomeBuildItem curateOutcome,
            OutputTargetBuildItem outputTarget,
            ApplicationInfoBuildItem applicationInfo,
            PackageConfig packageConfig,
            MainClassBuildItem mainClass,
            ApplicationArchivesBuildItem applicationArchives,
            TransformedClassesBuildItem transformedClasses,
            List<GeneratedClassBuildItem> generatedClasses,
            List<GeneratedResourceBuildItem> generatedResources,
            Set<ArtifactKey> removedArtifactKeys,
            ExecutorService executorService,
            ResolvedJVMRequirements jvmRequirements) {
        super(curateOutcome, outputTarget, applicationInfo, packageConfig, mainClass, applicationArchives, transformedClasses,
                generatedClasses, generatedResources, removedArtifactKeys, executorService, jvmRequirements);
    }

    public JarBuildItem build() throws IOException {
        Path runnerJar = outputTarget.getOutputDirectory()
                .resolve(outputTarget.getBaseName() + packageConfig.computedRunnerSuffix() + DOT_JAR);
        Path libDir = outputTarget.getOutputDirectory().resolve(LegacyThinJarFormat.LIB);
        Files.deleteIfExists(runnerJar);
        IoUtils.createOrEmptyDir(libDir);

        LOG.info("Building thin jar: " + runnerJar);

        doBuild(runnerJar, libDir);

        return new JarBuildItem(runnerJar, null, libDir, packageConfig.jar().type(),
                suffixToClassifier(packageConfig.computedRunnerSuffix()));
    }
}
