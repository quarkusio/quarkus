package io.quarkus.jacoco.deployment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
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

        String dataFile = getFilePath(config.dataFile(), outputTargetBuildItem.getOutputDirectory(),
                JacocoConfig.JACOCO_QUARKUS_EXEC);
        System.setProperty("jacoco-agent.destfile", dataFile);
        if (!config.reuseDataFile()) {
            Files.deleteIfExists(Paths.get(dataFile));
        }

        Instrumenter instrumenter = new Instrumenter(new OfflineInstrumentationAccessGenerator());
        Set<String> seen = new HashSet<>();
        for (ApplicationArchive archive : applicationArchivesBuildItem.getAllApplicationArchives()) {
            for (ClassInfo i : archive.getIndex().getKnownClasses()) {
                String className = i.name().toString();
                if (seen.contains(className)) {
                    continue;
                }
                seen.add(className);
                transformers.produce(
                        new BytecodeTransformerBuildItem.Builder().setClassToTransform(className)
                                .setCacheable(true)
                                .setInputTransformer(new BiFunction<String, byte[], byte[]>() {
                                    @Override
                                    public byte[] apply(String className, byte[] bytes) {
                                        try {
                                            byte[] enhanced = instrumenter.instrument(bytes, className);
                                            if (enhanced == null) {
                                                return bytes;
                                            }
                                            return enhanced;
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                }).build());
            }
        }
        if (config.report()) {
            ReportInfo info = new ReportInfo();
            info.dataFile = dataFile;

            File targetdir = new File(
                    getFilePath(config.reportLocation(), outputTargetBuildItem.getOutputDirectory(),
                            JacocoConfig.JACOCO_REPORT));
            info.reportDir = targetdir.getAbsolutePath();
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
                if (d.isRuntimeCp() && d.isWorkspaceModule()) {
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

    private String getFilePath(Optional<String> path, Path outputDirectory, String defaultRelativePath) {
        return path.orElse(outputDirectory.toAbsolutePath() + File.separator + defaultRelativePath);
    }
}
