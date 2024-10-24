package io.quarkus.jacoco.deployment;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

import org.codehaus.plexus.util.FileUtils;
import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.runtime.OfflineInstrumentationAccessGenerator;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.utils.BuildToolHelper;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.IsTest;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.index.IndexDependencyConfig;
import io.quarkus.deployment.index.IndexingUtil;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.jacoco.runtime.JacocoConfig;
import io.quarkus.jacoco.runtime.ReportCreator;
import io.quarkus.jacoco.runtime.ReportInfo;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.OpenPathTree;

public class JacocoProcessor {

    private static final Logger log = Logger.getLogger(JacocoProcessor.class);

    @BuildStep(onlyIf = IsTest.class)
    FeatureBuildItem feature() {
        return new FeatureBuildItem("jacoco");
    }

    @BuildStep(onlyIf = IsTest.class)
    void transform(BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformers,
            OutputTargetBuildItem outputTargetBuildItem,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            BuildSystemTargetBuildItem buildSystemTargetBuildItem,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            LaunchModeBuildItem launchModeBuildItem,
            JacocoConfig config) throws Exception {
        if (launchModeBuildItem.isAuxiliaryApplication()) {
            //no code coverage for continuous testing, it does not really make sense
            return;
        }
        if (!config.enabled()) {
            log.debug("quarkus-jacoco is disabled via config");
            return;
        }

        Path projectRoot = BuildToolHelper.getApplicationModuleOrCurrentDirectory(curateOutcomeBuildItem.getApplicationModel());
        if (projectRoot == null) {
            throw new IllegalStateException("Unable to find the project root");
        }
        Path outputDir = outputTargetBuildItem.getOutputDirectory().toAbsolutePath();

        // JaCoCo destFile - path to the output file for execution data
        Path dataFilePath;
        if (config.dataFile().isPresent()) {
            dataFilePath = projectRoot.resolve(config.dataFile().get());
        } else {
            dataFilePath = outputDir.resolve(JacocoConfig.JACOCO_QUARKUS_EXEC);
        }
        String dataFile = dataFilePath.toString();
        log.debugf("JaCoCo destFile: %s", dataFilePath);

        System.setProperty("jacoco-agent.destfile", dataFile);
        System.setProperty("jacoco-agent.jmx", "true");

        // Use offline instrumentation to modify the bytecode of classes that should be analyzed
        Set<ApplicationArchive> appArchives = applicationArchivesBuildItem.getAllApplicationArchives();
        ApplicationModel appModel = curateOutcomeBuildItem.getApplicationModel();
        Instrumenter instrumenter = new Instrumenter(new OfflineInstrumentationAccessGenerator());
        Set<String> transformed = new HashSet<>();
        Collection<IndexDependencyConfig> instrumentArtifacts = config.instrumentArtifacts().values();
        Set<ArtifactKey> instrumented = new HashSet<>();

        if (instrumentArtifacts.isEmpty()) {
            // By default, instrument classes from all application archives
            for (ApplicationArchive archive : appArchives) {
                instrument(instrumenter, transformed, archive.getIndex(), bytecodeTransformers);
            }
        } else {
            // Instrument only artifacts from the config
            for (IndexDependencyConfig artifact : instrumentArtifacts) {
                IndexView index = null;
                for (ApplicationArchive archive : appArchives) {
                    if (archiveMatches(archive.getKey(), artifact.groupId(), artifact.artifactId(), artifact.classifier())) {
                        index = archive.getIndex();
                        instrumented.add(archive.getKey());
                        log.debugf("Instrument app archive %s", archive.getKey());
                        break;
                    }
                }
                if (index == null) {
                    // Not an app archive - make sure it's a resolved dependency and build the index
                    for (ResolvedDependency d : appModel.getDependencies()) {
                        if (archiveMatches(d.getKey(), artifact.groupId(), artifact.artifactId(), artifact.classifier())) {
                            try (OpenPathTree openTree = d.getContentTree().open()) {
                                index = IndexingUtil.indexTree(openTree, null);
                                instrumented.add(d.getKey());
                                log.debugf("Instrument non-app archive artifact %s:%s", d.getGroupId(), d.getArtifactId());
                            } catch (IOException ioe) {
                                throw new UncheckedIOException(ioe);
                            }
                            break;
                        }
                    }
                }
                if (index != null) {
                    instrument(instrumenter, transformed, index, bytecodeTransformers);
                }
            }
        }

        // Generating a report is a bit tricky, especially if we want to aggregate data from several modules.
        // JaCoCo writes the coverage data on VM shutdown by default.
        // So we register a shutdown hook that waits for the JaCoCo data file and generates the report afterwards.
        // In a single-module Quarkus app/extension all we need is to prevent multiple shutdown hook registrations
        // for a single test suite execution (all tests executed in a single module).
        // Note that for @QuarkusTest a new build can be triggered e.g. by @TestProfile.
        // And for @QuarkusUnitTest each test class triggers a separate build.
        // So the system property check below will help for single module.
        // However, it will not help for multi-module projects if a shared data file is used:
        //
        // /my-extension-project
        // │
        // ├── /target/jacoco-quarkus.exec
        // ├── /foo
        // │   ├── runtime
        // │   ├── deployment
        // └── /bar (depends on foo)
        //     ├── runtime
        //     └── deployment
        //
        // Where each deployment module runs the test suite (in a separate VM), instruments classes
        // and registers a shutdown hook to generate a report.
        //
        // For this use case, the JacocoConfig#aggregateReportData() must be set in order to serialize
        // the source directories and class files for the report so that each ReportCreator can see the same configuration.

        String sysPropName = "io.quarkus.internal.jacoco.report-data-file";
        String currentDataFile = System.setProperty(sysPropName, dataFile);
        if (currentDataFile != null) {
            if (!currentDataFile.equals(dataFile)) {
                System.err.println("Quarkus will use the Jacoco data file " + currentDataFile
                        + ", not the configured data file " + dataFile + ", because another build step triggered a report.");
            }
            return;
        }

        if (!config.reuseDataFile()) {
            Files.deleteIfExists(dataFilePath);
        }

        if (config.report()) {
            ReportInfo info = new ReportInfo(config.aggregateReportData());
            info.dataFile = dataFilePath;

            Path reportDir;
            if (config.reportLocation().isPresent()) {
                reportDir = projectRoot.resolve(config.reportLocation().get());
            } else {
                reportDir = outputDir.resolve(JacocoConfig.JACOCO_REPORT);
            }
            log.debugf("JaCoCo report dir: %s", reportDir);

            info.reportDir = reportDir.toString();
            info.errorFile = reportDir.resolve("error.txt");
            Files.deleteIfExists(info.errorFile);
            info.artifactId = buildSystemTargetBuildItem.getBaseName();

            ReportCreator reportCreator = new ReportCreator(info, config);

            String includes = String.join(",", config.includes());
            String excludes = String.join(",", config.excludes().orElse(Collections.emptyList()));

            // Add classes and sources for the current build
            if (instrumentArtifacts.isEmpty()) {
                if (appModel.getApplicationModule() != null) {
                    addDependency(appModel.getAppArtifact(), config, projectRoot, info, includes, excludes, null);
                }
                for (ResolvedDependency d : appModel.getRuntimeDependencies()) {
                    // we can't use d.isWorkspaceModule() for now for some Gradle projects, which is why we check whether a workspace module is not null
                    if (d.getWorkspaceModule() != null) {
                        addDependency(d, config, projectRoot, info, includes, excludes, null);
                    }
                }
            } else {
                // For instrumented artifacts we always use the current data file when processing a dependency
                for (ResolvedDependency d : appModel.getRuntimeDependencies()) {
                    if (d.getWorkspaceModule() != null
                            && instrumented.contains(d.getKey())) {
                        addDependency(d, config, projectRoot, info, includes, excludes, dataFilePath);
                    }
                }
            }

            Path reportSourcesFilePath = dataFilePath.getParent().resolve("report-sources.txt");
            Path reportClassesFilePath = dataFilePath.getParent().resolve("report-classes.txt");

            // Add classes and sources from previous builds
            if (Files.isReadable(reportSourcesFilePath)) {
                for (String line : Files.readAllLines(reportSourcesFilePath)) {
                    String source = line.strip();
                    if (!source.isEmpty()) {
                        info.sourceDirectories.add(source);
                    }
                }
            }
            if (Files.isReadable(reportClassesFilePath)) {
                for (String line : Files.readAllLines(reportClassesFilePath)) {
                    String classFile = line.strip();
                    if (!classFile.isEmpty()) {
                        info.classFiles.add(classFile);
                    }
                }
            }

            // Write the current data
            Files.write(reportSourcesFilePath, info.sourceDirectories);
            Files.write(reportClassesFilePath, info.classFiles);

            Runtime.getRuntime().addShutdownHook(new Thread(reportCreator));
        }
    }

    private void addDependency(ResolvedDependency module, JacocoConfig config, Path projectRoot, ReportInfo info,
            String includes, String excludes, Path dataFile) throws Exception {
        if (dataFile == null) {
            dataFile = getFilePath(config.dataFile(), projectRoot, module.getWorkspaceModule().getBuildDir().toPath(),
                    JacocoConfig.JACOCO_QUARKUS_EXEC);
        }
        info.savedData.add(dataFile.toAbsolutePath().toString());
        if (module.getSources() == null) {
            return;
        }
        for (SourceDir src : module.getSources().getSourceDirs()) {
            for (Path p : src.getSourceTree().getRoots()) {
                info.sourceDirectories.add(p.toAbsolutePath().toString());
            }
            if (Files.isDirectory(src.getOutputDir())) {
                for (final File file : FileUtils.getFiles(src.getOutputDir().toFile(), includes, excludes,
                        true)) {
                    if (file.getName().endsWith(".class")) {
                        info.classFiles.add(file.getAbsolutePath());
                    }
                }
            }
        }
    }

    private static Path getFilePath(Optional<String> path, Path projectRoot, Path outputDirectory,
            String defaultRelativePath) {
        if (path.isPresent()) {
            return projectRoot.resolve(path.get());
        } else {
            return outputDirectory.resolve(defaultRelativePath);
        }
    }

    private void instrument(Instrumenter instrumenter, Set<String> transformed, IndexView index,
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformers) {
        for (ClassInfo i : index.getKnownClasses()) {
            String className = i.name().toString();
            if (transformed.add(className)) {
                BytecodeTransformerBuildItem bytecodeTransformer = new BytecodeTransformerBuildItem.Builder()
                        .setClassToTransform(className)
                        .setCacheable(true)
                        .setInputTransformer(new BiFunction<>() {
                            @Override
                            public byte[] apply(String className, byte[] bytes) {
                                try {
                                    byte[] enhanced = instrumenter.instrument(bytes, className);
                                    if (enhanced == null) {
                                        return bytes;
                                    }
                                    return enhanced;
                                } catch (IOException e) {
                                    if (!log.isDebugEnabled()) {
                                        log.warnf(
                                                "Unable to instrument class %s with JaCoCo: %s, keeping the original class",
                                                className, e.getMessage());
                                    } else {
                                        log.warnf(e,
                                                "Unable to instrument class %s with JaCoCo, keeping the original class",
                                                className);
                                    }
                                    return bytes;
                                }
                            }
                        }).build();
                bytecodeTransformers.produce(bytecodeTransformer);
            }

        }
    }

    public static boolean archiveMatches(ArtifactKey key, String groupId, Optional<String> artifactId,
            Optional<String> classifier) {
        if (key != null && Objects.equals(key.getGroupId(), groupId)
                && (artifactId.isEmpty() || Objects.equals(key.getArtifactId(), artifactId.get()))) {
            if (classifier.isPresent() && Objects.equals(key.getClassifier(), classifier.get())) {
                return true;
            } else if (!classifier.isPresent() && ArtifactCoords.DEFAULT_CLASSIFIER.equals(key.getClassifier())) {
                return true;
            }
        }
        return false;
    }
}
