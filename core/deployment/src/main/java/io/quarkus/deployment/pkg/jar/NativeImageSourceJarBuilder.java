package io.quarkus.deployment.pkg.jar;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.deployment.ResolvedJVMRequirements;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedNativeImageClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.MainClassBuildItem;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageSourceJarBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.GACT;

public class NativeImageSourceJarBuilder extends AbstractLegacyThinJarBuilder<NativeImageSourceJarBuildItem> {

    private static final Logger LOG = Logger.getLogger(NativeImageSourceJarBuilder.class);

    public NativeImageSourceJarBuilder(CurateOutcomeBuildItem curateOutcome,
            OutputTargetBuildItem outputTarget,
            ApplicationInfoBuildItem applicationInfo,
            PackageConfig packageConfig,
            MainClassBuildItem mainClass,
            ApplicationArchivesBuildItem applicationArchives,
            TransformedClassesBuildItem transformedClasses,
            List<GeneratedClassBuildItem> generatedClasses,
            List<GeneratedResourceBuildItem> generatedResources,
            List<GeneratedNativeImageClassBuildItem> nativeImageResources,
            Set<ArtifactKey> removedArtifactKeys,
            ExecutorService executorService,
            ResolvedJVMRequirements jvmRequirements) {
        super(curateOutcome, outputTarget, applicationInfo, packageConfig, mainClass, applicationArchives, transformedClasses,
                augmentGeneratedClasses(generatedClasses, nativeImageResources), generatedResources,
                augmentRemovedArtifactKeys(removedArtifactKeys), executorService, jvmRequirements);
    }

    public NativeImageSourceJarBuildItem build() throws IOException {
        Path targetDirectory = outputTarget.getOutputDirectory()
                .resolve(outputTarget.getBaseName() + "-native-image-source-jar");
        IoUtils.createOrEmptyDir(targetDirectory);

        Path runnerJar = targetDirectory
                .resolve(outputTarget.getBaseName() + packageConfig.computedRunnerSuffix() + DOT_JAR);
        Path libDir = targetDirectory.resolve(LegacyThinJarFormat.LIB);
        Files.createDirectories(libDir);

        copyJsonConfigFiles(applicationArchives, targetDirectory);

        // complain if graal-sdk is present as a dependency as nativeimage should be preferred
        if (curateOutcome.getApplicationModel().getDependencies().stream()
                .anyMatch(d -> d.getGroupId().equals("org.graalvm.sdk") && d.getArtifactId().equals("graal-sdk"))) {
            LOG.warn("org.graalvm.sdk:graal-sdk is present in the classpath. "
                    + "From Quarkus 3.8 and onwards, org.graalvm.sdk:nativeimage should be preferred. "
                    + "Make sure you report the issue to the maintainers of the extensions that bring it.");
        }

        LOG.info("Building native image source jar: " + runnerJar);

        doBuild(runnerJar, libDir);

        return new NativeImageSourceJarBuildItem(runnerJar, libDir);
    }

    private static List<GeneratedClassBuildItem> augmentGeneratedClasses(List<GeneratedClassBuildItem> generatedClasses,
            List<GeneratedNativeImageClassBuildItem> nativeImageResources) {
        List<GeneratedClassBuildItem> allGeneratedClasses = new ArrayList<>(generatedClasses);
        allGeneratedClasses.addAll(nativeImageResources.stream()
                .map((s) -> new GeneratedClassBuildItem(true, s.getName(), s.getClassData()))
                .toList());
        return allGeneratedClasses;
    }

    private static Set<ArtifactKey> augmentRemovedArtifactKeys(Set<ArtifactKey> removedArtifactKeys) {
        final Set<ArtifactKey> allRemovedArtifactKeys = new HashSet<>(removedArtifactKeys);
        // Remove svm and graal-sdk artifacts as they are provided by GraalVM itself
        allRemovedArtifactKeys.add(GACT.fromString("org.graalvm.nativeimage:svm"));
        allRemovedArtifactKeys.add(GACT.fromString("org.graalvm.sdk:graal-sdk"));
        allRemovedArtifactKeys.add(GACT.fromString("org.graalvm.sdk:nativeimage"));
        allRemovedArtifactKeys.add(GACT.fromString("org.graalvm.sdk:word"));
        allRemovedArtifactKeys.add(GACT.fromString("org.graalvm.sdk:collections"));
        return allRemovedArtifactKeys;
    }

    /**
     * This is done in order to make application specific native image configuration files available to the native-image tool
     * without the user needing to know any specific paths.
     * The files that are copied don't end up in the native image unless the user specifies they are needed, all this method
     * does is copy them to a convenient location
     */
    private static void copyJsonConfigFiles(ApplicationArchivesBuildItem applicationArchivesBuildItem, Path thinJarDirectory)
            throws IOException {
        for (Path root : applicationArchivesBuildItem.getRootArchive().getRootDirectories()) {
            try (Stream<Path> stream = Files.find(root, 1, IsJsonFilePredicate.INSTANCE)) {
                stream.forEach(new Consumer<Path>() {
                    @Override
                    public void accept(Path jsonPath) {
                        try {
                            Files.createDirectories(thinJarDirectory);
                            Files.copy(jsonPath, thinJarDirectory.resolve(jsonPath.getFileName().toString()));
                        } catch (IOException e) {
                            throw new UncheckedIOException(
                                    "Unable to copy json config file from " + jsonPath + " to " + thinJarDirectory,
                                    e);
                        }
                    }
                });
            }
        }
    }

    private static class IsJsonFilePredicate implements BiPredicate<Path, BasicFileAttributes> {

        private static final BiPredicate<Path, BasicFileAttributes> INSTANCE = new IsJsonFilePredicate();

        @Override
        public boolean test(Path path, BasicFileAttributes basicFileAttributes) {
            return basicFileAttributes.isRegularFile() && path.toString().endsWith(".json");
        }
    }
}
