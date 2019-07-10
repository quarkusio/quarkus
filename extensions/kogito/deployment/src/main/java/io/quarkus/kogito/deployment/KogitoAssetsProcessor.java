package io.quarkus.kogito.deployment;

import java.io.ByteArrayInputStream;
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
import org.drools.compiler.kproject.models.KieModuleModelImpl;
import org.drools.modelcompiler.builder.JavaParserCompiler;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.kogito.codegen.ApplicationGenerator;
import org.kie.kogito.codegen.GeneratedFile;
import org.kie.kogito.codegen.di.CDIDependencyInjectionAnnotator;
import org.kie.kogito.codegen.process.ProcessCodegen;
import org.kie.kogito.codegen.rules.IncrementalRuleCodegen;
import org.kie.kogito.codegen.rules.RuleCodegen;

import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ArchiveRootBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.runtime.LaunchMode;

public class KogitoAssetsProcessor {

    private final transient String generatedClassesDir = System.getProperty("quarkus.debug.generated-classes-dir");

    @BuildStep(providesCapabilities = "io.quarkus.kogito")
    FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem(FeatureBuildItem.KOGITO);
    }

    @BuildStep
    public void generateModel(ArchiveRootBuildItem root,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            CombinedIndexBuildItem combinedIndexBuildItem,
            LaunchModeBuildItem launchMode,
            LiveReloadBuildItem liveReload) throws IOException {

        if (liveReload.isLiveReload()) {
            return;
        }

        boolean generateRuleUnits = true;
        boolean generateProcesses = true;

        ApplicationGenerator appGen = createApplicationGenerator(root, launchMode.getLaunchMode(), generateRuleUnits,
                generateProcesses, combinedIndexBuildItem);
        Collection<GeneratedFile> generatedFiles = appGen.generate();

        if (!generatedFiles.isEmpty()) {
            MemoryFileSystem trgMfs = new MemoryFileSystem();
            CompilationResult result = compile(root, trgMfs, generatedFiles, generatedBeans, launchMode.getLaunchMode());
            register(trgMfs, generatedBeans, launchMode.getLaunchMode(), result);
        }
    }

    private CompilationResult compile(ArchiveRootBuildItem root, MemoryFileSystem trgMfs,
            Collection<GeneratedFile> generatedFiles,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans, LaunchMode launchMode)
            throws IOException {

        JavaCompiler javaCompiler = JavaParserCompiler.getCompiler();
        JavaCompilerSettings compilerSettings = javaCompiler.createDefaultSettings();
        compilerSettings.addClasspath(root.getPath().toString());

        MemoryFileSystem srcMfs = new MemoryFileSystem();

        String[] sources = new String[generatedFiles.size()];
        int index = 0;
        for (GeneratedFile entry : generatedFiles) {
            String fileName = toRuntimeSource(toClassName(entry.relativePath()));
            sources[index++] = fileName;

            srcMfs.write(fileName, entry.contents());

            writeGeneratedFile(entry);
        }

        return javaCompiler.compile(sources, srcMfs, trgMfs,
                Thread.currentThread().getContextClassLoader(), compilerSettings);
    }

    private void register(MemoryFileSystem trgMfs, BuildProducer<GeneratedBeanBuildItem> generatedBeans, LaunchMode launchMode,
            CompilationResult result) throws IOException {
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
            boolean generateRuleUnits, boolean generateProcesses, CombinedIndexBuildItem combinedIndexBuildItem)
            throws IOException {
        Path targetClassesPath = root.getArchiveLocation();
        Path projectPath = targetClassesPath.toString().endsWith("target" + File.separator + "classes")
                ? targetClassesPath.getParent().getParent()
                : targetClassesPath;

        String appPackageName = "org.kie.kogito.app";

        ApplicationGenerator appGen = new ApplicationGenerator(appPackageName, new File(projectPath.toFile(), "target"))
                .withDependencyInjection(new CDIDependencyInjectionAnnotator());

        if (generateRuleUnits) {
            Path moduleXmlPath = projectPath.resolve("src/main/resources").resolve(KieModuleModelImpl.KMODULE_JAR_PATH);
            KieModuleModel kieModuleModel = KieModuleModelImpl.fromXML(
                    new ByteArrayInputStream(
                            Files.readAllBytes(moduleXmlPath)));

            appGen.withGenerator(IncrementalRuleCodegen.ofPath(projectPath))
                    .withRuleEventListenersConfig(customRuleEventListenerConfigExists(projectPath, appPackageName,
                            combinedIndexBuildItem.getIndex()))
                    .withKModule(kieModuleModel)
                    .withClassLoader(Thread.currentThread().getContextClassLoader());
        }

        if (generateProcesses) {
            appGen.withGenerator(ProcessCodegen.ofPath(projectPath))
                    .withWorkItemHandlerConfig(
                            customWorkItemConfigExists(projectPath, appPackageName, combinedIndexBuildItem.getIndex()))
                    .withProcessEventListenerConfig(
                            customProcessListenerConfigExists(projectPath, appPackageName, combinedIndexBuildItem.getIndex()));
        }

        return appGen;
    }

    private String customWorkItemConfigExists(Path projectPath, String appPackageName, IndexView index) {
        String workItemHandlerConfigClass = ProcessCodegen.defaultWorkItemHandlerConfigClass(appPackageName);

        ClassInfo workItemHandlerConfigClassInfo = index
                .getClassByName(createDotName(workItemHandlerConfigClass));

        return workItemHandlerConfigClassInfo != null ? workItemHandlerConfigClass : null;
    }

    private String customProcessListenerConfigExists(Path projectPath, String appPackageName, IndexView index) {
        String processEventListenerClass = ProcessCodegen.defaultProcessListenerConfigClass(appPackageName);
        ClassInfo processEventListenerClassInfo = index
                .getClassByName(createDotName(processEventListenerClass));

        return processEventListenerClassInfo != null ? processEventListenerClass : null;
    }

    private String customRuleEventListenerConfigExists(Path projectPath, String appPackageName, IndexView index) {
        String ruleEventListenerConfiglass = RuleCodegen.defaultRuleEventListenerConfigClass(appPackageName);
        ClassInfo ruleEventListenerConfiglassInfo = index
                .getClassByName(createDotName(ruleEventListenerConfiglass));

        return ruleEventListenerConfiglassInfo != null ? ruleEventListenerConfiglass : null;
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

    private DotName createDotName(String name) {
        int lastDot = name.indexOf('.');
        if (lastDot < 0) {
            return DotName.createComponentized(null, name);
        }

        DotName lastDotName = null;
        while (lastDot > 0) {
            String local = name.substring(0, lastDot);
            name = name.substring(lastDot + 1);
            lastDot = name.indexOf('.');
            lastDotName = DotName.createComponentized(lastDotName, local);
        }

        return DotName.createComponentized(lastDotName, name);
    }

}
