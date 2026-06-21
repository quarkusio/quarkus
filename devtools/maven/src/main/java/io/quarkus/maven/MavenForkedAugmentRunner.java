package io.quarkus.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.toolchain.ToolchainManager;

import io.quarkus.bootstrap.app.ApplicationModelSerializer;
import io.quarkus.bootstrap.app.AugmentResult;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.maven.toolchain.MavenToolchains;
import io.quarkus.maven.worker.MavenAugmentWorker;
import io.quarkus.maven.worker.MavenAugmentWorkerConfig;
import io.quarkus.maven.worker.PluginClassPath;
import io.quarkus.runtime.LaunchMode;
import io.smallrye.common.os.OS;

/**
 * Runs Quarkus augmentation or code generation in a JVM forked from the configured Maven toolchain JDK.
 */
final class MavenForkedAugmentRunner {

    private static final List<String> JVM_ARGS = List.of(
            "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-exports=java.base/jdk.internal.module=ALL-UNNAMED");

    private MavenForkedAugmentRunner() {
    }

    static boolean shouldFork(QuarkusBootstrapMojo mojo) {
        return MavenToolchains.shouldFork(mojo.toolchainManager(), mojo.mavenSession());
    }

    static AugmentResult runBuild(QuarkusBootstrapMojo mojo, ApplicationModel appModel, Properties buildProperties,
            Log log) throws MojoExecutionException {
        Path workDir = createWorkDir(mojo);
        try {
            Path appModelPath = workDir.resolve("app-model.dat");
            ApplicationModelSerializer.serialize(appModel, appModelPath);
            Path buildPropertiesPath = writeProperties(workDir.resolve("build.properties"), buildProperties);
            Path resultPath = workDir.resolve("build-result.properties");
            Path configPath = MavenAugmentWorkerConfig.write(
                    workDir.resolve("worker.config"),
                    MavenAugmentWorkerConfig.MODE_BUILD,
                    appModelPath,
                    buildPropertiesPath,
                    mojo.baseDir().toPath(),
                    mojo.buildDir().toPath(),
                    mojo.finalName(),
                    mojo.mavenProject().getBuild().getFinalName(),
                    resultPath,
                    null,
                    LaunchMode.NORMAL.name(),
                    false,
                    Collections.emptyList());
            runWorker(mojo, configPath, log);
            return MavenAugmentWorkerConfig.readBuildResult(resultPath);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to prepare forked Quarkus build", e);
        } finally {
            deleteQuietly(workDir);
        }
    }

    static void runCodegen(QuarkusBootstrapMojo mojo,
            ApplicationModel appModel,
            Properties buildProperties,
            Path generatedSourcesDir,
            String launchMode,
            boolean test,
            List<Path> sourceParents,
            Log log) throws MojoExecutionException {
        Path workDir = createWorkDir(mojo);
        try {
            Path appModelPath = workDir.resolve("app-model.dat");
            ApplicationModelSerializer.serialize(appModel, appModelPath);
            Path buildPropertiesPath = writeProperties(workDir.resolve("build.properties"), buildProperties);
            Path configPath = MavenAugmentWorkerConfig.write(
                    workDir.resolve("worker.config"),
                    MavenAugmentWorkerConfig.MODE_CODEGEN,
                    appModelPath,
                    buildPropertiesPath,
                    mojo.baseDir().toPath(),
                    mojo.buildDir().toPath(),
                    mojo.finalName(),
                    mojo.mavenProject().getBuild().getFinalName(),
                    null,
                    generatedSourcesDir,
                    launchMode,
                    test,
                    sourceParents);
            runWorker(mojo, configPath, log);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to prepare forked Quarkus code generation", e);
        } finally {
            deleteQuietly(workDir);
        }
    }

    private static void runWorker(QuarkusBootstrapMojo mojo, Path configPath, Log log) throws MojoExecutionException {
        ToolchainManager toolchainManager = mojo.toolchainManager();
        String javaExecutable = MavenToolchains.findJavaExecutable(toolchainManager, mojo.mavenSession())
                .orElseThrow(() -> new MojoExecutionException(
                        "Unable to locate the Java executable from the configured Maven toolchain"));
        log.info("Using JVM from Maven toolchain for Quarkus build: " + javaExecutable);

        List<String> command = new ArrayList<>();
        command.add(javaExecutable);
        command.addAll(JVM_ARGS);
        command.add("-classpath");
        command.add(PluginClassPath.from(MavenAugmentWorker.class.getClassLoader()));
        command.add(MavenAugmentWorker.class.getName());
        command.add(configPath.toString());

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(mojo.baseDir());
        processBuilder.environment().put("MAVEN_CMD_LINE_ARGS", "");
        configureToolchainEnvironment(processBuilder, javaExecutable);

        try {
            Process process = processBuilder.start();
            Thread stdout = streamToLog(process.getInputStream(), log, false);
            Thread stderr = streamToLog(process.getErrorStream(), log, true);
            stdout.start();
            stderr.start();
            int exitCode = process.waitFor();
            stdout.join();
            stderr.join();
            if (exitCode != 0) {
                throw new MojoExecutionException(
                        "Forked Quarkus build failed with exit code " + exitCode + " using JVM " + javaExecutable);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MojoExecutionException("Forked Quarkus build was interrupted", e);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to launch forked Quarkus build using JVM " + javaExecutable, e);
        }
    }

    private static void configureToolchainEnvironment(ProcessBuilder processBuilder, String javaExecutable) {
        Path javaBinPath = javaExecutable == null ? null : java.nio.file.Paths.get(javaExecutable).getParent();
        if (javaBinPath == null) {
            return;
        }
        Path javaHome = javaBinPath.getParent();
        if (javaHome == null) {
            return;
        }
        processBuilder.environment().put("JAVA_HOME", javaHome.toString());
        if (OS.current() == OS.WINDOWS) {
            String javaBin = javaBinPath.toAbsolutePath().toString();
            String path = processBuilder.environment().getOrDefault("PATH", "");
            processBuilder.environment().put("PATH", javaBin + File.pathSeparator + path);
        }
    }

    private static Thread streamToLog(InputStream inputStream, Log log, boolean error) {
        return new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (error) {
                        log.error(line);
                    } else {
                        log.info(line);
                    }
                }
            } catch (IOException ignored) {
            }
        }, error ? "quarkus-worker-stderr" : "quarkus-worker-stdout");
    }

    private static Path createWorkDir(QuarkusBootstrapMojo mojo) throws MojoExecutionException {
        try {
            return Files.createTempDirectory(mojo.buildDir().toPath(), "quarkus-maven-worker-");
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to create temporary worker directory", e);
        }
    }

    private static Path writeProperties(Path target, Properties properties) throws IOException {
        try (OutputStream out = Files.newOutputStream(target)) {
            properties.store(out, "Quarkus build system properties");
        }
        return target;
    }

    private static void deleteQuietly(Path workDir) {
        if (workDir == null) {
            return;
        }
        try {
            Files.walk(workDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }
}
