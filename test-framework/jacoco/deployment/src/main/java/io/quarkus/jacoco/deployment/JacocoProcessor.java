package io.quarkus.jacoco.deployment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

import org.codehaus.plexus.util.FileUtils;
import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.runtime.OfflineInstrumentationAccessGenerator;
import org.jboss.jandex.ClassInfo;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.IsTest;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.jacoco.runtime.JacocoConfig;
import io.quarkus.jacoco.runtime.ReportCreator;
import io.quarkus.jacoco.runtime.ReportInfo;
import io.quarkus.maven.dependency.ResolvedDependency;

public class JacocoProcessor {

    private static final Logger log = Logger.getLogger(JacocoProcessor.class);

    @BuildStep(onlyIf = IsTest.class)
    FeatureBuildItem feature() {
        return new FeatureBuildItem("jacoco");
    }

    private static final Map<String, BytecodeTransformerBuildItem> transformedClasses = new HashMap<>();

    @BuildStep(onlyIf = IsTest.class)
    void transformerBuildItem(BuildProducer<BytecodeTransformerBuildItem> transformers,
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

        Path outputDir = outputTargetBuildItem.getOutputDirectory().toAbsolutePath();
        Files.createDirectories(outputDir);

        Path dataFilePath = outputDir.resolve(config.dataFile().orElse(JacocoConfig.JACOCO_QUARKUS_EXEC));
        String dataFile = dataFilePath.toString();

        System.setProperty("jacoco-agent.destfile", dataFile);
        System.setProperty("jacoco-agent.jmx", "true");

        Instrumenter instrumenter = new Instrumenter(new OfflineInstrumentationAccessGenerator());
        for (ApplicationArchive archive : applicationArchivesBuildItem.getAllApplicationArchives()) {
            for (ClassInfo i : archive.getIndex().getKnownClasses()) {
                String className = i.name().toString();
                BytecodeTransformerBuildItem bytecodeTransformerBuildItem = transformedClasses.get(className);
                if (bytecodeTransformerBuildItem == null) {
                    bytecodeTransformerBuildItem = new BytecodeTransformerBuildItem.Builder().setClassToTransform(className)
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
                    transformedClasses.put(className, bytecodeTransformerBuildItem);
                }
                transformers.produce(bytecodeTransformerBuildItem);
            }
        }

        String sysPropName = "io.quarkus.internal.jacoco.report-data-file";
        String currentDataFile = System.setProperty(sysPropName, dataFile);
        if (currentDataFile != null) {
            if (!currentDataFile.equals(dataFile)) {
                System.err.println("Quarkus will use the Jacoco data file " + currentDataFile
                        + ", not the configured data file " + dataFile + ", because another build item triggered a report.");
            }
            return;
        }

        if (!config.reuseDataFile()) {
            Files.deleteIfExists(dataFilePath);
        }

        if (config.report()) {
            ReportInfo info = new ReportInfo();
            info.dataFile = dataFilePath;

            Path targetPath = outputDir.resolve(config.reportLocation().orElse(JacocoConfig.JACOCO_REPORT));
            info.reportDir = targetPath.toString();
            info.errorFile = targetPath.resolve("error.txt");
            Files.deleteIfExists(info.errorFile);
            String includes = String.join(",", config.includes());
            String excludes = String.join(",", config.excludes().orElse(Collections.emptyList()));
            Set<String> classes = new HashSet<>();
            info.classFiles = classes;

            Set<String> sources = new HashSet<>();
            final ApplicationModel model = curateOutcomeBuildItem.getApplicationModel();
            if (model.getApplicationModule() != null) {
                addProjectModule(model.getAppArtifact(), config, info, includes, excludes, classes, sources);
            }
            for (ResolvedDependency d : model.getDependencies()) {
                // we can't use d.isWorkspaceModule() for now for some Gradle projects, which is why we check whether a workspace module is not null
                if (d.isRuntimeCp() && d.getWorkspaceModule() != null) {
                    addProjectModule(d, config, info, includes, excludes, classes, sources);
                }
            }

            info.sourceDirectories = sources;
            info.artifactId = buildSystemTargetBuildItem.getBaseName();
            Runtime.getRuntime().addShutdownHook(new Thread(new ReportCreator(info, config)));
        }
    }

    private void addProjectModule(ResolvedDependency module, JacocoConfig config, ReportInfo info, String includes,
            String excludes, Set<String> classes, Set<String> sources) throws Exception {
        String dataFile = getFilePath(config.dataFile(), module.getWorkspaceModule().getBuildDir().toPath(),
                JacocoConfig.JACOCO_QUARKUS_EXEC);
        info.savedData.add(new File(dataFile).getAbsolutePath());
        if (module.getSources() == null) {
            return;
        }
        for (SourceDir src : module.getSources().getSourceDirs()) {
            for (Path p : src.getSourceTree().getRoots()) {
                sources.add(p.toAbsolutePath().toString());
            }
            if (Files.isDirectory(src.getOutputDir())) {
                for (final File file : FileUtils.getFiles(src.getOutputDir().toFile(), includes, excludes,
                        true)) {
                    if (file.getName().endsWith(".class")) {
                        classes.add(file.getAbsolutePath());
                    }
                }
            }
        }
    }

    private static String getFilePath(Optional<String> path, Path outputDirectory, String defaultRelativePath) {
        return path.orElse(outputDirectory.toAbsolutePath() + File.separator + defaultRelativePath);
    }
}
