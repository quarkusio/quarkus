/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.kogito.deployment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;

import org.drools.compiler.commons.jci.compilers.CompilationResult;
import org.drools.compiler.commons.jci.compilers.JavaCompiler;
import org.drools.compiler.commons.jci.compilers.JavaCompilerSettings;
import org.drools.compiler.compiler.io.memory.MemoryFileSystem;
import org.drools.modelcompiler.builder.JavaParserCompiler;
import org.kie.kogito.codegen.ApplicationGenerator;
import org.kie.kogito.codegen.GeneratedFile;
import org.kie.kogito.codegen.process.ProcessCodegen;
import org.kie.kogito.codegen.rules.RuleCodegen;

import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ArchiveRootBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.runtime.LaunchMode;

public class KogitoAssetsProcessor {

    private static boolean IS_HOT_RELOAD = false;

    private final transient String generatedClassesDir = System.getProperty("quarkus.debug.generated-classes-dir");

    @BuildStep(providesCapabilities = "io.quarkus.kogito")
    FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem("kogito");
    }

    @BuildStep
    public void generateModel(ArchiveRootBuildItem root,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            LaunchModeBuildItem launchMode) throws IOException {

        if (hotReload(launchMode.getLaunchMode())) {
            return;
        }

        boolean generateRuleUnits = true;
        boolean generateProcesses = true;

        ApplicationGenerator appGen = createApplicationGenerator(root, launchMode.getLaunchMode(), generateRuleUnits,
                generateProcesses);

        Collection<GeneratedFile> generatedFiles = appGen.generate();

        compileAndRegister(root, generatedFiles, generatedBeans, launchMode.getLaunchMode());
    }

    private boolean hotReload(LaunchMode launchMode) {
        if (launchMode == LaunchMode.DEVELOPMENT) {
            boolean hotReload = IS_HOT_RELOAD;
            IS_HOT_RELOAD = true;
            return hotReload;
        }
        return false;
    }

    private void compileAndRegister(ArchiveRootBuildItem root, Collection<GeneratedFile> generatedFiles,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            LaunchMode launchMode)
            throws IOException {
        if (generatedFiles.isEmpty()) {
            return;
        }

        JavaCompiler javaCompiler = JavaParserCompiler.getCompiler();
        JavaCompilerSettings compilerSettings = javaCompiler.createDefaultSettings();
        compilerSettings.addClasspath(root.getPath().toString());

        MemoryFileSystem srcMfs = new MemoryFileSystem();
        MemoryFileSystem trgMfs = new MemoryFileSystem();

        String[] sources = new String[generatedFiles.size()];
        int index = 0;
        for (GeneratedFile entry : generatedFiles) {
            String fileName = toRuntimeSource(toClassName(entry.relativePath()));
            sources[index++] = fileName;

            srcMfs.write(fileName, entry.contents());

            writeGeneratedFile(entry);
        }

        CompilationResult result = javaCompiler.compile(sources, srcMfs, trgMfs,
                Thread.currentThread().getContextClassLoader(), compilerSettings);

        if (result.getErrors().length > 0) {
            StringBuilder errorInfo = new StringBuilder();
            Arrays.stream(result.getErrors()).forEach(cp -> errorInfo.append(cp.toString()));
            throw new IllegalStateException(errorInfo.toString());
        }

        for (String fileName : trgMfs.getFileNames()) {
            byte[] data = trgMfs.getBytes(fileName);
            String className = toClassName(fileName);
            generatedBeans.produce(new GeneratedBeanBuildItem(className, data));

            if (launchMode == LaunchMode.DEVELOPMENT) {
                writeFile(fileName, data);
            }
        }
    }

    private void writeFile(String fileName, byte[] data) throws IOException {
        Path path = Paths.get(fileName);
        if (!Files.exists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }
        Files.write(path, data);
    }

    private ApplicationGenerator createApplicationGenerator(ArchiveRootBuildItem root, LaunchMode launchMode,
            boolean generateRuleUnits, boolean generateProcesses) throws IOException {
        Path targetClassesPath = root.getPath();
        Path projectPath = targetClassesPath.toString().endsWith("target/classes") ? targetClassesPath.getParent().getParent()
                : targetClassesPath;

        String appPackageName = "org.kie.kogito.app";

        ApplicationGenerator appGen = new ApplicationGenerator(appPackageName, new File(projectPath.toFile(), "target"))
                .withDependencyInjection(true);

        if (generateRuleUnits) {
            appGen.withGenerator(RuleCodegen.ofPath(projectPath, launchMode == LaunchMode.DEVELOPMENT))
                    .withRuleEventListenersConfig(customRuleEventListenerConfigExists(projectPath, appPackageName));
        }

        if (generateProcesses) {
            appGen.withGenerator(ProcessCodegen.ofPath(projectPath))
                    .withWorkItemHandlerConfig(
                            customWorkItemConfigExists(projectPath, appPackageName))
                    .withProcessEventListenerConfig(
                            customProcessListenerConfigExists(projectPath, appPackageName));
        }

        return appGen;
    }

    private String customWorkItemConfigExists(Path projectPath, String appPackageName) {
        String sourceDir = Paths.get(projectPath.toString(), "src").toString();
        String workItemHandlerConfigClass = ProcessCodegen.defaultWorkItemHandlerConfigClass(appPackageName);
        Path p = Paths.get(sourceDir,
                "main/java",
                workItemHandlerConfigClass.replace('.', '/') + ".java");
        return Files.exists(p) ? workItemHandlerConfigClass : null;
    }

    private String customProcessListenerConfigExists(Path projectPath, String appPackageName) {
        String sourceDir = Paths.get(projectPath.toString(), "src").toString();
        String processEventListenerClass = ProcessCodegen.defaultProcessListenerConfigClass(appPackageName);
        Path p = Paths.get(sourceDir,
                "main/java",
                processEventListenerClass.replace('.', '/') + ".java");
        return Files.exists(p) ? processEventListenerClass : null;
    }

    private String customRuleEventListenerConfigExists(Path projectPath, String appPackageName) {
        String sourceDir = Paths.get(projectPath.toString(), "src").toString();
        String ruleEventListenerConfiglass = RuleCodegen.defaultRuleEventListenerConfigClass(appPackageName);
        Path p = Paths.get(sourceDir,
                "main/java",
                ruleEventListenerConfiglass.replace('.', '/') + ".java");
        return Files.exists(p) ? ruleEventListenerConfiglass : null;
    }

    private String toRuntimeSource(String className) {
        return "src/main/java/" + className.replace('.', '/') + ".java";
    }

    private String toClassName(String sourceName) {
        if (sourceName.startsWith("./")) {
            sourceName = sourceName.substring(2);
        }
        if (sourceName.endsWith(".java")) {
            sourceName = sourceName.substring(0, sourceName.length() - 5);
        } else if (sourceName.endsWith(".class")) {
            sourceName = sourceName.substring(0, sourceName.length() - 6);
        }
        return sourceName.replace('/', '.');
    }

    private void writeGeneratedFile(GeneratedFile f) throws IOException {
        if (generatedClassesDir == null) {
            return;
        }

        Files.write(
                pathOf(f.relativePath()),
                f.contents());
    }

    private Path pathOf(String end) {
        Path path = Paths.get(generatedClassesDir, end);
        path.getParent().toFile().mkdirs();
        return path;
    }
}
