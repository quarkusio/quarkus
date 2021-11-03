package io.quarkus.jacoco.deployment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.runtime.OfflineInstrumentationAccessGenerator;
import org.jboss.jandex.ClassInfo;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.utils.BuildToolHelper;
import io.quarkus.bootstrap.workspace.ProcessedSources;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
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

        String dataFile = outputTargetBuildItem.getOutputDirectory().toAbsolutePath().toString() + File.separator
                + config.dataFile;
        System.setProperty("jacoco-agent.destfile",
                dataFile);
        if (!config.reuseDataFile) {
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
                                .setEager(true)
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
        if (config.report) {
            ReportInfo info = new ReportInfo();
            info.dataFile = dataFile;

            File targetdir = new File(
                    outputTargetBuildItem.getOutputDirectory().toAbsolutePath().toString() + File.separator
                            + config.reportLocation);
            info.reportDir = targetdir.getAbsolutePath();
            String includes = StringUtils.join(config.includes.iterator(), ",");
            String excludes = StringUtils.join(config.excludes.orElse(Collections.emptyList()).iterator(), ",");
            Set<String> classes = new HashSet<>();
            info.classFiles = classes;

            Set<String> sources = new HashSet<>();
            ApplicationModel model;
            if (BuildToolHelper.isMavenProject(targetdir.toPath())) {
                model = curateOutcomeBuildItem.getApplicationModel();
            } else if (BuildToolHelper.isGradleProject(targetdir.toPath())) {
                //this seems counter productive, but we want the dev mode model and not the test model
                //as the test model will include the test classes that we don't want in the report
                model = BuildToolHelper.enableGradleAppModelForDevMode(targetdir.toPath());
            } else {
                throw new RuntimeException("Cannot determine project type generating Jacoco report");
            }

            if (model.getApplicationModule() != null) {
                addProjectModule(model.getApplicationModule(), config, info, includes, excludes, classes, sources);
            }
            for (ResolvedDependency d : model.getDependencies()) {
                if (d.isRuntimeCp() && d.isWorkspacetModule()) {
                    addProjectModule(d.getWorkspaceModule(), config, info, includes, excludes, classes, sources);
                }
            }

            info.sourceDirectories = sources;
            info.artifactId = buildSystemTargetBuildItem.getBaseName();
            Runtime.getRuntime().addShutdownHook(new Thread(new ReportCreator(info, config)));
        }
    }

    private void addProjectModule(WorkspaceModule module, JacocoConfig config, ReportInfo info, String includes,
            String excludes, Set<String> classes, Set<String> sources) throws Exception {
        info.savedData.add(new File(module.getBuildDir(), config.dataFile).getAbsolutePath());
        for (ProcessedSources src : module.getMainSources()) {
            sources.add(src.getSourceDir().getAbsolutePath());
            if (src.getDestinationDir().isDirectory()) {
                for (final File file : FileUtils.getFiles(src.getDestinationDir(), includes, excludes,
                        true)) {
                    if (file.getName().endsWith(".class")) {
                        classes.add(file.getAbsolutePath());
                    }
                }
            }
        }
    }
}
