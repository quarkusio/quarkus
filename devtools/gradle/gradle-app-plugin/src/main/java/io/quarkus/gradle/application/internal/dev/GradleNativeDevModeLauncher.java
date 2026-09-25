package io.quarkus.gradle.application.internal.dev;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.app.ConfiguredClassLoading;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.devmode.DependenciesFilter;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.deployment.dev.DevModeCommandLine;
import io.quarkus.deployment.dev.DevModeCommandLineBuilder;
import io.quarkus.deployment.dev.DevModeContext;
import io.quarkus.deployment.dev.ExtensionDevModeJvmOptionFilter;
import io.quarkus.deployment.dev.IsolatedTestModeMain;
import io.quarkus.gradle.application.internal.launch.ConsoleColorSupport;
import io.quarkus.gradle.application.model.QuarkusApplicationDevDebugMode;
import io.quarkus.gradle.tooling.ToolingUtils;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathList;

public final class GradleNativeDevModeLauncher {

    private GradleNativeDevModeLauncher() {
    }

    static QuarkusApplicationDevProcessHandle launch(Parameters parameters,
            DevModeContext.ExternalBuildOutputTransport transport) throws Exception {
        ApplicationModel model = ToolingUtils.deserializeAppModel(parameters.applicationModel());
        ApplicationModel testModel = parameters.testApplicationModel() == null ? null
                : ToolingUtils.deserializeAppModel(parameters.testApplicationModel());
        DevModeCommandLine runner = buildCommandLine(parameters, model, testModel, transport);
        Process process = processBuilder(parameters, runner.getArguments()).start();
        var devUiUrl = new AtomicReference<String>();
        return new ProcessHandle(process,
                startOutputPump(process.getInputStream(), System.out, devUiUrl),
                startOutputPump(process.getErrorStream(), System.err, devUiUrl),
                devUiUrl);
    }

    static ProcessBuilder processBuilder(Parameters parameters, List<String> arguments) {
        ProcessBuilder processBuilder = new ProcessBuilder(arguments)
                .directory(parameters.workingDirectory().toFile());
        processBuilder.environment().putAll(parameters.environmentVariables());
        return processBuilder;
    }

    private static DevModeCommandLine buildCommandLine(Parameters parameters, ApplicationModel model,
            ApplicationModel testModel,
            DevModeContext.ExternalBuildOutputTransport transport) throws Exception {
        var builder = commandLineBuilder(parameters.javaExecutable())
                .projectDir(parameters.projectDirectory().toFile())
                .buildDir(parameters.buildDirectory().toFile())
                .outputDir(parameters.buildDirectory().toFile())
                .buildSystemProperties(parameters.quarkusBuildProperties())
                .applicationName(parameters.applicationName())
                .applicationVersion(parameters.applicationVersion())
                .extensionDevModeConfig(model.getExtensionDevModeConfig())
                .entryPointCustomizer(context -> {
                    context.setBuildUpdateSource(DevModeContext.BuildUpdateSource.EXTERNAL_BUILD_TOOL);
                    context.setExternalBuildOutputTransport(transport);
                    context.setExternalTestApplicationModel(testModel);
                    if (parameters.continuousTestOnly()) {
                        context.setMode(QuarkusBootstrap.Mode.CONTINUOUS_TEST);
                        context.setTest(true);
                        context.setAlternateEntryPoint(IsolatedTestModeMain.class.getName());
                    }
                });
        applyLaunchOptions(builder, parameters);
        builder.jvmArgs("-Dquarkus.console.basic=true")
                .jvmArgs(ConsoleColorSupport.jvmArgument(parameters.forcePlainConsole(),
                        parameters.quarkusBuildProperties().get(ConsoleColorSupport.FORCE_COLOR_SUPPORT_PROPERTY)))
                .jvmArgs("-Dquarkus.console.disable-input=true")
                .jvmArgs("-Dquarkus.test.continuous-testing="
                        + (parameters.continuousTesting() ? "enabled" : "disabled"))
                .jvmArgs("-Dgradle.project.path=" + parameters.projectDirectory().toAbsolutePath())
                .jvmArgs("-Dquarkus.live-reload.instrumentation=false");
        for (Map.Entry<String, String> entry : parameters.devSystemProperties().entrySet()) {
            builder.jvmArgs("-D" + entry.getKey() + "=" + entry.getValue());
        }
        for (String jvmArg : parameters.devJvmArgs()) {
            builder.jvmArgs(jvmArg);
        }
        for (String jvmArg : parameters.jvmArguments()) {
            builder.jvmArgs(jvmArg);
        }
        if (!parameters.applicationArguments().isEmpty()) {
            builder.applicationArgs(String.join(" ", parameters.applicationArguments()));
        }
        if (parameters.openJavaLang()) {
            builder.addOpens("java.base/java.lang=ALL-UNNAMED");
        }
        if (!parameters.modules().isEmpty()) {
            builder.addModules(parameters.modules());
        }
        if (!parameters.compilerArguments().isEmpty()) {
            builder.compilerOptions("java", parameters.compilerArguments());
        }
        if (!parameters.tests().isEmpty()) {
            builder.jvmArgs("-Dquarkus-internal.test.specific-selection=gradle:"
                    + String.join(",", parameters.tests()));
        }
        String serializedModelProperty = parameters.continuousTestOnly()
                ? BootstrapConstants.SERIALIZED_TEST_APP_MODEL
                : BootstrapConstants.SERIALIZED_APP_MODEL;
        builder.jvmArgs("-D" + serializedModelProperty + "=" + parameters.applicationModel().toAbsolutePath());

        Set<ArtifactKey> localDependencies = new LinkedHashSet<>();
        for (ResolvedDependency dependency : DependenciesFilter.getReloadableModules(model)) {
            addLocalModule(builder, dependency, localDependencies, parameters,
                    model.getAppArtifact().getWorkspaceModule().getId()
                            .equals(dependency.getWorkspaceModule().getId()));
        }
        for (File dependency : parameters.devModeClasspath()) {
            builder.classpathEntry(ArtifactKey.of("io.quarkus.gradle.application", dependency.getName(), null, "jar"),
                    dependency);
        }
        Set<Path> resourceDirs = new HashSet<>();
        if (model.getApplicationModule() != null && model.getApplicationModule().getMainSources() != null) {
            for (SourceDir resourceDir : model.getApplicationModule().getMainSources().getResourceDirs()) {
                resourceDirs.add(resourceDir.getOutputDir());
            }
        }
        Collection<ArtifactKey> configuredParentFirst = ConfiguredClassLoading.builder()
                .setApplicationModel(model)
                .setApplicationRoot(PathsCollection.from(resourceDirs))
                .setMode(QuarkusBootstrap.Mode.DEV)
                .build()
                .getParentFirstArtifacts();
        for (ResolvedDependency dependency : model.getDependencies()) {
            if (!localDependencies.contains(dependency.getKey()) && configuredParentFirst.contains(dependency.getKey())) {
                addDependencyClasspathEntries(builder, dependency);
            }
        }
        return builder.build();
    }

    static DevModeCommandLineBuilder commandLineBuilder(Path javaExecutable) {
        return DevModeCommandLine.builder(javaExecutable.toString());
    }

    static void applyLaunchOptions(DevModeCommandLineBuilder builder, Parameters parameters) {
        ExtensionDevModeJvmOptionFilter extensionJvmOptions = new ExtensionDevModeJvmOptionFilter();
        extensionJvmOptions.setDisableAll(parameters.disableAllExtensionJvmOptions());
        extensionJvmOptions.setDisableFor(parameters.disableExtensionJvmOptionsFor());
        builder.forceC2(parameters.forceC2())
                .debug(debugValue(parameters.debug(), parameters.debugMode()))
                .debugHost(parameters.debugHost())
                .debugPort(parameters.debugPort() == null ? null : parameters.debugPort().toString())
                .suspend(parameters.suspend() == null ? null : parameters.suspend().toString())
                .extensionDevModeJvmOptionFilter(extensionJvmOptions);
    }

    static String debugValue(Boolean debug, QuarkusApplicationDevDebugMode debugMode) {
        if (Boolean.FALSE.equals(debug)) {
            return "false";
        }
        if (debugMode == QuarkusApplicationDevDebugMode.CONNECT) {
            return "client";
        }
        if (Boolean.TRUE.equals(debug) || debugMode == QuarkusApplicationDevDebugMode.LISTEN) {
            return "true";
        }
        return null;
    }

    private static void addDependencyClasspathEntries(io.quarkus.deployment.dev.DevModeCommandLineBuilder builder,
            ResolvedDependency dependency) {
        for (Path path : dependency.getResolvedPaths()) {
            File file = path.toFile();
            if (file.exists()) {
                builder.classpathEntry(dependency.getKey(), file);
            }
        }
    }

    private static void addLocalModule(io.quarkus.deployment.dev.DevModeCommandLineBuilder builder,
            ResolvedDependency dependency, Set<ArtifactKey> localDependencies, Parameters parameters, boolean root) {
        localDependencies.add(dependency.getKey());
        ArtifactSources sources = dependency.getSources();
        if (sources == null) {
            return;
        }
        WorkspaceModule module = dependency.getWorkspaceModule();
        if (module == null) {
            return;
        }
        Set<Path> sourcePaths = new LinkedHashSet<>();
        Set<Path> sourceParents = new LinkedHashSet<>();
        Set<Path> classesDirs = new LinkedHashSet<>();
        for (SourceDir source : sources.getSourceDirs()) {
            if (Files.exists(source.getDir())) {
                sourcePaths.add(source.getDir());
                sourceParents.add(source.getDir().getParent());
                classesDirs.add(source.getOutputDir());
            }
        }
        Path resourcesOutputDir = null;
        Set<Path> resourcePaths = new LinkedHashSet<>();
        for (SourceDir resource : sources.getResourceDirs()) {
            resourcePaths.add(resource.getDir());
            if (resourcesOutputDir == null) {
                resourcesOutputDir = resource.getOutputDir();
            }
        }
        if (classesDirs.isEmpty() && resourcesOutputDir != null) {
            classesDirs.add(resourcesOutputDir);
        }
        if (classesDirs.isEmpty()) {
            return;
        }
        Path classesDir = classesDirs.iterator().next();
        Path resourcesOutputPath = resourcesOutputDir == null ? classesDir : resourcesOutputDir;
        DevModeContext.ModuleInfo.Builder moduleInfo = new DevModeContext.ModuleInfo.Builder()
                .setArtifactKey(dependency.getKey())
                .setName(dependency.getArtifactId())
                .setProjectDirectory(module.getModuleDir().getAbsolutePath())
                .setSourcePaths(PathList.from(sourcePaths))
                .setClassesPaths(classesDirs)
                .setResourcePaths(PathList.from(resourcePaths))
                .setResourcesOutputPath(resourcesOutputPath.toString())
                .setSourceParents(PathList.from(sourceParents))
                .setPreBuildOutputDir(module.getBuildDir().toPath().resolve("generated-sources").toAbsolutePath().toString())
                .setTargetDir(module.getBuildDir().toString());
        if (root && parameters.continuousTesting()) {
            moduleInfo.setTestSourcePaths(PathList.from(toPaths(parameters.testSourceDirectories())));
            // Kotlin plugins can contribute optional class-output roots which remain absent when no processor emits
            // binary classes there. Passing such a phantom root makes continuous testing create it during startup;
            // a later KSP cleanup then removes it underneath the reusable test application class loader.
            moduleInfo.setTestClassesPaths(existingPaths(parameters.testClasses()));
            Path testResources = firstPath(parameters.testResources());
            moduleInfo.setTestResourcesOutputPath(testResources == null ? null : testResources.toString());
        }
        DevModeContext.ModuleInfo builtModuleInfo = moduleInfo.build();
        if (root) {
            builder.mainModule(builtModuleInfo);
        } else {
            builder.dependency(builtModuleInfo);
        }
    }

    private static List<Path> toPaths(Collection<File> files) {
        return files.stream().map(File::toPath).toList();
    }

    static List<Path> existingPaths(Collection<File> files) {
        return files.stream().map(File::toPath).filter(Files::exists).toList();
    }

    private static Path firstPath(Collection<File> files) {
        return files.stream().map(File::toPath).findFirst().orElse(null);
    }

    public record Parameters(
            Path javaExecutable,
            Path applicationModel,
            Path testApplicationModel,
            boolean continuousTestOnly,
            boolean continuousTesting,
            Collection<File> devModeClasspath,
            Collection<File> testSourceDirectories,
            Collection<File> testClasses,
            Collection<File> testResources,
            Path projectDirectory,
            Path buildDirectory,
            Path workingDirectory,
            String applicationName,
            String applicationVersion,
            Map<String, String> quarkusBuildProperties,
            List<String> devJvmArgs,
            List<String> jvmArguments,
            List<String> applicationArguments,
            List<String> modules,
            boolean openJavaLang,
            List<String> compilerArguments,
            List<String> tests,
            boolean forcePlainConsole,
            Map<String, String> devSystemProperties,
            Map<String, String> environmentVariables,
            Boolean debug,
            QuarkusApplicationDevDebugMode debugMode,
            String debugHost,
            Integer debugPort,
            Boolean suspend,
            Boolean forceC2,
            boolean disableAllExtensionJvmOptions,
            List<String> disableExtensionJvmOptionsFor) {
        public Parameters {
            javaExecutable = javaExecutable.toAbsolutePath().normalize();
            applicationModel = applicationModel.normalize();
            testApplicationModel = testApplicationModel == null ? null : testApplicationModel.normalize();
            projectDirectory = projectDirectory.toAbsolutePath().normalize();
            buildDirectory = buildDirectory.toAbsolutePath().normalize();
            workingDirectory = workingDirectory.toAbsolutePath().normalize();
            devModeClasspath = List.copyOf(devModeClasspath);
            testSourceDirectories = List.copyOf(testSourceDirectories);
            testClasses = List.copyOf(testClasses);
            testResources = List.copyOf(testResources);
            quarkusBuildProperties = Map.copyOf(quarkusBuildProperties);
            devJvmArgs = List.copyOf(devJvmArgs);
            jvmArguments = List.copyOf(jvmArguments);
            applicationArguments = List.copyOf(applicationArguments);
            modules = List.copyOf(modules);
            compilerArguments = List.copyOf(compilerArguments);
            tests = List.copyOf(tests);
            devSystemProperties = Map.copyOf(devSystemProperties);
            environmentVariables = Map.copyOf(environmentVariables);
            disableExtensionJvmOptionsFor = List.copyOf(disableExtensionJvmOptionsFor);
        }
    }

    private static Thread startOutputPump(InputStream stream, PrintStream target, AtomicReference<String> devUiUrl) {
        Thread thread = new Thread(() -> {
            try (stream; var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    QuarkusDevUiUrlParser.parse(line).ifPresent(url -> devUiUrl.compareAndSet(null, url));
                    target.println("[quarkus-dev] " + line);
                }
            } catch (IOException e) {
                target.println("[quarkus-dev] stopped reading dev process output: " + e.getMessage());
            }
        }, "quarkus-application-dev-output");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static final class ProcessHandle implements QuarkusApplicationDevProcessHandle {

        private final Process process;
        private final List<Thread> outputPumps;
        private final AtomicReference<String> devUiUrl;
        private final CompletableFuture<Integer> exitCode;
        private final AtomicBoolean closing = new AtomicBoolean();

        private ProcessHandle(Process process, Thread outputPump, Thread errorPump, AtomicReference<String> devUiUrl) {
            this.process = process;
            this.outputPumps = List.of(outputPump, errorPump);
            this.devUiUrl = devUiUrl;
            this.exitCode = process.onExit().thenApply(Process::exitValue);
        }

        @Override
        public boolean isAlive() {
            return process.isAlive();
        }

        @Override
        public CompletionStage<Integer> exitCode() {
            return exitCode;
        }

        @Override
        public Optional<String> devUiUrl() {
            return Optional.ofNullable(devUiUrl.get());
        }

        @Override
        public void close() throws IOException {
            if (!closing.compareAndSet(false, true)) {
                return;
            }
            process.destroy();
            try {
                if (!process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    if (!process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                        throw new IOException("Timed out while forcibly stopping Quarkus dev mode process");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while stopping Quarkus dev mode process", e);
            }
            joinOutputPumps();
        }

        private void joinOutputPumps() throws IOException {
            List<InterruptedException> interruptions = new ArrayList<>();
            for (Thread outputPump : outputPumps) {
                try {
                    outputPump.join(1000);
                    if (outputPump.isAlive()) {
                        throw new IOException("Timed out while stopping Quarkus dev mode output pump "
                                + outputPump.getName());
                    }
                } catch (InterruptedException e) {
                    interruptions.add(e);
                    Thread.currentThread().interrupt();
                }
            }
            if (!interruptions.isEmpty()) {
                IOException failure = new IOException("Interrupted while stopping Quarkus dev mode output pumps");
                interruptions.forEach(failure::addSuppressed);
                throw failure;
            }
        }
    }
}
