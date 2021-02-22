package io.quarkus.jacoco.deployment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.runtime.OfflineInstrumentationAccessGenerator;
import org.jacoco.report.MultiSourceFileLocator;
import org.jboss.jandex.ClassInfo;

import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.resolver.model.QuarkusModel;
import io.quarkus.bootstrap.resolver.model.WorkspaceModule;
import io.quarkus.bootstrap.utils.BuildToolHelper;
import io.quarkus.deployment.IsTest;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.jacoco.runtime.JacocoConfig;
import io.quarkus.jacoco.runtime.ReportCreator;
import io.quarkus.jacoco.runtime.ReportInfo;

public class JacocoProcessor {

    @BuildStep(onlyIf = IsTest.class)
    FeatureBuildItem feature() {
        return new FeatureBuildItem("jacoco");
    }

    @BuildStep(onlyIf = IsTest.class)
    void transformerBuildItem(BuildProducer<BytecodeTransformerBuildItem> transformers, CombinedIndexBuildItem indexBuildItem,
            OutputTargetBuildItem outputTargetBuildItem,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            BuildSystemTargetBuildItem buildSystemTargetBuildItem,
            ShutdownContextBuildItem shutdownContextBuildItem,
            JacocoConfig config) throws Exception {
        String dataFile = outputTargetBuildItem.getOutputDirectory().toAbsolutePath().toString() + File.separator
                + config.dataFile;
        System.setProperty("jacoco-agent.destfile",
                dataFile);
        Files.deleteIfExists(Paths.get(dataFile));

        Instrumenter instrumenter = new Instrumenter(new OfflineInstrumentationAccessGenerator());
        for (ClassInfo i : indexBuildItem.getIndex().getKnownClasses()) {
            String className = i.name().toString();
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
        if (config.report) {
            ReportInfo info = new ReportInfo();
            info.savedData = dataFile;

            File targetdir = new File(
                    outputTargetBuildItem.getOutputDirectory().toAbsolutePath().toString() + File.separator
                            + config.reportLocation);
            info.reportDir = targetdir.getAbsolutePath();
            String includes = StringUtils.join(config.includes.iterator(), ",");
            String excludes = StringUtils.join(config.excludes.orElse(Collections.emptyList()).iterator(), ",");
            Set<String> classes = new HashSet<>();
            info.classFiles = classes;

            Set<String> sources = new HashSet<>();
            MultiSourceFileLocator sourceFileLocator = new MultiSourceFileLocator(4);
            if (BuildToolHelper.isMavenProject(targetdir.toPath())) {
                LocalProject project = LocalProject.loadWorkspace(targetdir.toPath());
                for (Map.Entry<AppArtifactKey, LocalProject> i : project.getWorkspace().getProjects().entrySet()) {
                    sources.add(i.getValue().getSourcesSourcesDir().toFile().getAbsolutePath());
                    File classesDir = i.getValue().getClassesDir().toFile();
                    if (classesDir.isDirectory()) {
                        for (final File file : FileUtils.getFiles(classesDir, includes, excludes,
                                true)) {
                            if (file.getName().endsWith(".class")) {
                                classes.add(file.getAbsolutePath());
                            }
                        }
                    }
                }
            } else if (BuildToolHelper.isGradleProject(targetdir.toPath())) {
                QuarkusModel model = BuildToolHelper.enableGradleAppModelForTest(targetdir.toPath());
                for (WorkspaceModule i : model.getWorkspace().getAllModules()) {
                    for (File src : i.getSourceSourceSet().getSourceDirectories()) {
                        sources.add(src.getAbsolutePath());
                    }
                    for (File classesDir : i.getSourceSet().getSourceDirectories()) {
                        if (classesDir.isDirectory()) {
                            for (final File file : FileUtils.getFiles(classesDir, includes, excludes,
                                    true)) {
                                if (file.getName().endsWith(".class")) {
                                    classes.add(file.getAbsolutePath());
                                }
                            }
                        }
                    }
                }
            } else {
                throw new RuntimeException("Cannot determine project type generating Jacoco report");
            }
            info.sourceDirectories = sources;
            info.artifactId = buildSystemTargetBuildItem.getBaseName();
            Runtime.getRuntime().addShutdownHook(new Thread(new ReportCreator(info, config)));
        }
    }

}
