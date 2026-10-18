package io.quarkus.deployment.pkg.steps;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.util.IoUtils;

/**
 * JAR tree-shake phase 3 environment: a temp directory pre-populated with generated/transformed
 * bytecode, the {@link ClassLoadingRecorder} class, and JVM arguments. Created once
 * and reused across iterations of the fixed-point loop so that bytecode is written
 * to disk only once.
 */
final class ForkedJvmEnvironment implements AutoCloseable {

    private static final Logger log = Logger.getLogger(ForkedJvmEnvironment.class.getName());

    private final Path tempDir;
    private final Path inputFile;
    private final String javaCmd;
    private final Path argFile;

    /**
     * Prepares the forked JVM environment: creates a temp directory, writes bytecode,
     * the recorder class, and JVM arguments.
     *
     * @param generatedBytecode generated classes (in-memory only, must be written to disk)
     * @param transformedBytecode transformed classes that override originals in dep JARs
     * @param depJarPaths file system paths to dependency JARs
     * @param appPaths file system paths to the application artifact
     */
    static ForkedJvmEnvironment create(
            Map<String, Supplier<byte[]>> generatedBytecode,
            Map<String, Supplier<byte[]>> transformedBytecode,
            List<Path> depJarPaths,
            List<Path> appPaths) throws IOException {

        Path tempDir = Files.createTempDirectory("tree-shake-recording");
        try {
            writeBytecodeToDir(tempDir, generatedBytecode);
            writeBytecodeToDir(tempDir, transformedBytecode);
            writeRecorderClass(tempDir);

            StringBuilder cpBuilder = new StringBuilder();
            cpBuilder.append(tempDir);
            for (Path p : depJarPaths) {
                cpBuilder.append(File.pathSeparator).append(p);
            }
            for (Path p : appPaths) {
                cpBuilder.append(File.pathSeparator).append(p);
            }

            Path argFile = tempDir.resolve("_jvm.args");
            Files.writeString(argFile,
                    "-cp\n" + cpBuilder + "\n-XX:MaxMetaspaceSize=256m",
                    StandardCharsets.UTF_8);

            String javaCmd = ProcessHandle.current().info().command()
                    .orElse(Path.of(System.getProperty("java.home"), "bin", "java").toString());

            Path inputFile = tempDir.resolve("_input.txt");

            if (log.isDebugEnabled()) {
                log.debugf("Forked JVM environment created: %d generated + %d transformed classes written to disk",
                        generatedBytecode.size(), transformedBytecode.size());
            }

            return new ForkedJvmEnvironment(tempDir, inputFile, javaCmd, argFile);
        } catch (IOException | RuntimeException e) {
            IoUtils.recursiveDelete(tempDir);
            throw e;
        }
    }

    private ForkedJvmEnvironment(Path tempDir, Path inputFile, String javaCmd, Path argFile) {
        this.tempDir = tempDir;
        this.inputFile = inputFile;
        this.javaCmd = javaCmd;
        this.argFile = argFile;
    }

    /**
     * Executes entry point classes in a forked JVM to capture which classes
     * get loaded during static initialization and construction.
     *
     * @param entryPointClasses classes whose init/clinit triggers dynamic class loading
     * @param allKnownClasses all known class names for Map value extraction and result filtering
     * @return discovered class names limited to {@code allKnownClasses}, or empty set on failure
     */
    Set<String> executeEntryPoints(Set<String> entryPointClasses,
            Set<String> allKnownClasses) throws IOException, InterruptedException {

        List<String> inputLines = new ArrayList<>(entryPointClasses.size() + allKnownClasses.size() + 1);
        inputLines.addAll(entryPointClasses);
        inputLines.add("---");
        inputLines.addAll(allKnownClasses);
        Files.write(inputFile, inputLines, StandardCharsets.UTF_8);

        ProcessBuilder pb = new ProcessBuilder(
                javaCmd,
                "@" + argFile,
                ClassLoadingRecorder.class.getName(),
                inputFile.toString());
        pb.redirectErrorStream(false);

        Process proc = pb.start();

        Thread stderrDrainer = new Thread(() -> {
            try {
                proc.getErrorStream().transferTo(OutputStream.nullOutputStream());
            } catch (IOException ignored) {
            }
        }, "tree-shake-stderr-drainer");
        stderrDrainer.setDaemon(true);
        stderrDrainer.start();

        Set<String> discovered = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty() && allKnownClasses.contains(line)) {
                    discovered.add(line);
                }
            }
        }

        int exitCode = proc.waitFor();
        if (exitCode != 0) {
            log.warnf("Class-loading chain analysis forked JVM exited with code %d", exitCode);
        }
        stderrDrainer.join(5000);

        return discovered;
    }

    @Override
    public void close() {
        IoUtils.recursiveDelete(tempDir);
    }

    private static void writeBytecodeToDir(Path dir, Map<String, Supplier<byte[]>> bytecodeMap) throws IOException {
        for (var entry : bytecodeMap.entrySet()) {
            byte[] bytes;
            try {
                bytes = entry.getValue().get();
            } catch (Exception e) {
                continue;
            }
            String classFile = entry.getKey().replace('.', '/') + ".class";
            Path target = dir.resolve(classFile);
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        }
    }

    /**
     * Writes the {@link ClassLoadingRecorder} class files to the temp directory.
     */
    private static void writeRecorderClass(Path tempDir) throws IOException {
        String baseName = ClassLoadingRecorder.class.getName().replace('.', '/');
        for (String suffix : new String[] { ".class", "$RecordingClassLoader.class" }) {
            String resourcePath = baseName + suffix;
            try (var is = ForkedJvmEnvironment.class.getClassLoader().getResourceAsStream(resourcePath)) {
                if (is == null) {
                    if (suffix.equals(".class")) {
                        throw new IOException("Cannot find ClassLoadingRecorder.class on classpath");
                    }
                    continue;
                }
                Path target = tempDir.resolve(resourcePath);
                Files.createDirectories(target.getParent());
                Files.write(target, is.readAllBytes());
            }
        }
    }
}
