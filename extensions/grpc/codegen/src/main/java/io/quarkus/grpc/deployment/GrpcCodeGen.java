package io.quarkus.grpc.deployment;

import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.prebuild.CodeGenException;
import io.quarkus.bootstrap.prebuild.CodeGenFailureException;
import io.quarkus.deployment.CodeGenContext;
import io.quarkus.deployment.CodeGenProvider;
import io.quarkus.deployment.util.ProcessUtil;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.runtime.util.HashUtil;
import io.quarkus.utilities.JavaBinFinder;
import io.quarkus.utilities.OS;

/**
 * Code generation for gRPC. Generates java classes from proto files placed in either src/main/proto or src/test/proto
 * Inspired by <a href="https://github.com/xolstice/protobuf-maven-plugin">Protobuf Maven Plugin</a>
 */
public class GrpcCodeGen implements CodeGenProvider {
    private static final Logger log = Logger.getLogger(GrpcCodeGen.class);

    private static final String quarkusProtocPluginMain = "io.quarkus.grpc.protoc.plugin.MutinyGrpcGenerator";
    private static final String EXE = "exe";
    private static final String PROTO = ".proto";
    private static final String PROTOC = "protoc";
    private static final String PROTOC_GROUPID = "com.google.protobuf";

    private static final String SCAN_DEPENDENCIES_FOR_PROTO = "quarkus.generate-code.grpc.scan-for-proto";
    private static final String SCAN_FOR_IMPORTS = "quarkus.generate-code.grpc.scan-for-imports";

    private static final String POST_PROCESS_SKIP = "quarkus.generate.code.grpc-post-processing.skip";

    private Executables executables;

    @Override
    public String providerId() {
        return "grpc";
    }

    @Override
    public String inputExtension() {
        return "proto";
    }

    @Override
    public String inputDirectory() {
        return "proto";
    }

    @Override
    public boolean trigger(CodeGenContext context) throws CodeGenException {
        if (TRUE.toString().equalsIgnoreCase(System.getProperties().getProperty("grpc.codegen.skip", "false"))
                || context.config().getOptionalValue("quarkus.grpc.codegen.skip", Boolean.class).orElse(false)) {
            log.info("Skipping " + this.getClass() + " invocation on user's request");
            return false;
        }
        Path outDir = context.outDir();
        Path workDir = context.workDir();
        Set<String> protoDirs = new HashSet<>();

        try {
            List<String> protoFiles = new ArrayList<>();
            if (Files.isDirectory(context.inputDir())) {
                try (Stream<Path> protoFilesPaths = Files.walk(context.inputDir())) {
                    protoFilesPaths
                            .filter(Files::isRegularFile)
                            .filter(s -> s.toString().endsWith(PROTO))
                            .map(Path::normalize)
                            .map(Path::toAbsolutePath)
                            .map(Path::toString)
                            .forEach(protoFiles::add);
                    protoDirs.add(context.inputDir().normalize().toAbsolutePath().toString());
                }
            }
            Path dirWithProtosFromDependencies = workDir.resolve("protoc-protos-from-dependencies");
            Collection<Path> protoFilesFromDependencies = gatherProtosFromDependencies(dirWithProtosFromDependencies, protoDirs,
                    context);
            if (!protoFilesFromDependencies.isEmpty()) {
                protoFilesFromDependencies.stream()
                        .map(Path::normalize)
                        .map(Path::toAbsolutePath)
                        .map(Path::toString)
                        .forEach(protoFiles::add);
            }

            if (!protoFiles.isEmpty()) {
                initExecutables(workDir, context.applicationModel());

                Collection<String> protosToImport = gatherDirectoriesWithImports(workDir.resolve("protoc-dependencies"),
                        context);

                List<String> command = new ArrayList<>();
                command.add(executables.protoc.toString());
                for (String protoImportDir : protosToImport) {
                    command.add(String.format("-I=%s", escapeWhitespace(protoImportDir)));
                }
                for (String protoDir : protoDirs) {
                    command.add(String.format("-I=%s", escapeWhitespace(protoDir)));
                }

                command.addAll(asList("--plugin=protoc-gen-grpc=" + executables.grpc,
                        "--plugin=protoc-gen-q-grpc=" + executables.quarkusGrpc,
                        "--q-grpc_out=" + outDir,
                        "--grpc_out=" + outDir,
                        "--java_out=" + outDir));
                command.addAll(protoFiles);

                ProcessBuilder processBuilder = new ProcessBuilder(command);

                final Process process = ProcessUtil.launchProcess(processBuilder, context.shouldRedirectIO());
                int resultCode = process.waitFor();
                if (resultCode != 0) {
                    throw new CodeGenException("Failed to generate Java classes from proto files: " + protoFiles +
                            " to " + outDir.toAbsolutePath() + " with command " + String.join(" ", command));
                }
                postprocessing(context, outDir);
                log.info("Successfully finished generating and post-processing sources from proto files");
                return true;
            }
        } catch (IOException | InterruptedException e) {
            throw new CodeGenException(
                    "Failed to generate java files from proto file in " + context.inputDir().toAbsolutePath(), e);
        }

        return false;
    }

    private void postprocessing(CodeGenContext context, Path outDir) {
        if (TRUE.toString().equalsIgnoreCase(System.getProperties().getProperty(POST_PROCESS_SKIP, "false"))
                || context.config().getOptionalValue(POST_PROCESS_SKIP, Boolean.class).orElse(false)) {
            log.info("Skipping gRPC Post-Processing on user's request");
            return;
        }

        new GrpcPostProcessing(context, outDir).postprocess();

    }

    private Collection<Path> gatherProtosFromDependencies(Path workDir, Set<String> protoDirectories,
            CodeGenContext context) throws CodeGenException {
        if (context.test()) {
            return Collections.emptyList();
        }
        Config properties = context.config();
        String scanDependencies = properties.getOptionalValue(SCAN_DEPENDENCIES_FOR_PROTO, String.class)
                .orElse("none");

        if ("none".equalsIgnoreCase(scanDependencies)) {
            return Collections.emptyList();
        }
        boolean scanAll = "all".equalsIgnoreCase(scanDependencies);

        List<String> dependenciesToScan = asList(scanDependencies.split(","));

        ApplicationModel appModel = context.applicationModel();
        List<Path> protoFilesFromDependencies = new ArrayList<>();
        for (ResolvedDependency artifact : appModel.getRuntimeDependencies()) {
            if (scanAll
                    || dependenciesToScan.contains(
                            String.format("%s:%s", artifact.getGroupId(), artifact.getArtifactId()))) {
                extractProtosFromArtifact(workDir, protoFilesFromDependencies, protoDirectories, artifact);
            }
        }
        return protoFilesFromDependencies;
    }

    @Override
    public boolean shouldRun(Path sourceDir, Config config) {
        return CodeGenProvider.super.shouldRun(sourceDir, config)
                || isGeneratingFromAppDependenciesEnabled(config);
    }

    private boolean isGeneratingFromAppDependenciesEnabled(Config config) {
        return config.getOptionalValue(SCAN_DEPENDENCIES_FOR_PROTO, String.class)
                .filter(value -> !"none".equals(value)).isPresent();
    }

    private Collection<String> gatherDirectoriesWithImports(Path workDir, CodeGenContext context) throws CodeGenException {
        Config properties = context.config();

        String scanForImports = properties.getOptionalValue(SCAN_FOR_IMPORTS, String.class)
                .orElse("com.google.protobuf:protobuf-java");

        if ("none".equals(scanForImports.toLowerCase(Locale.getDefault()))) {
            return Collections.emptyList();
        }

        boolean scanAll = "all".equals(scanForImports.toLowerCase(Locale.getDefault()));
        List<String> dependenciesToScan = Arrays.asList(scanForImports.split(","));

        Set<String> importDirectories = new HashSet<>();
        ApplicationModel appModel = context.applicationModel();
        for (ResolvedDependency artifact : appModel.getRuntimeDependencies()) {
            if (scanAll
                    || dependenciesToScan.contains(
                            String.format("%s:%s", artifact.getGroupId(), artifact.getArtifactId()))) {
                extractProtosFromArtifact(workDir, new ArrayList<>(), importDirectories, artifact);
            }
        }
        return importDirectories;
    }

    private void extractProtosFromArtifact(Path workDir, Collection<Path> protoFiles,
            Set<String> protoDirectories, ResolvedDependency artifact) throws CodeGenException {

        try {
            artifact.getContentTree().walk(
                    pathVisit -> {
                        Path path = pathVisit.getPath();
                        if (Files.isRegularFile(path) && path.getFileName().toString().endsWith(PROTO)) {
                            Path root = pathVisit.getRoot();
                            if (Files.isDirectory(root)) {
                                protoFiles.add(path);
                                protoDirectories.add(path.getParent().normalize().toAbsolutePath().toString());
                            } else { // archive
                                Path relativePath = path.getRoot().relativize(path);
                                Path protoUnzipDir = workDir
                                        .resolve(HashUtil.sha1(root.normalize().toAbsolutePath().toString()))
                                        .normalize().toAbsolutePath();
                                try {
                                    Files.createDirectories(protoUnzipDir);
                                    protoDirectories.add(protoUnzipDir.toString());
                                } catch (IOException e) {
                                    throw new GrpcCodeGenException("Failed to create directory: " + protoUnzipDir, e);
                                }
                                Path outPath = protoUnzipDir;
                                for (Path part : relativePath) {
                                    outPath = outPath.resolve(part.toString());
                                }
                                try {
                                    Files.createDirectories(outPath.getParent());
                                    Files.copy(path, outPath, StandardCopyOption.REPLACE_EXISTING);
                                    protoFiles.add(outPath);
                                } catch (IOException e) {
                                    throw new GrpcCodeGenException("Failed to extract proto file" + path + " to target: "
                                            + outPath, e);
                                }
                            }
                        }
                    });
        } catch (GrpcCodeGenException e) {
            throw new CodeGenException(e.getMessage(), e);
        }
    }

    private String escapeWhitespace(String path) {
        if (OS.determineOS() == OS.LINUX) {
            return path.replace(" ", "\\ ");
        } else {
            return path;
        }
    }

    private void initExecutables(Path workDir, ApplicationModel model) throws CodeGenException {
        if (executables == null) {
            Path protocPath;
            String protocPathProperty = System.getProperty("quarkus.grpc.protoc-path");
            String classifier = System.getProperty("quarkus.grpc.protoc-os-classifier", osClassifier());
            if (protocPathProperty == null) {
                protocPath = findArtifactPath(model, PROTOC_GROUPID, PROTOC, classifier, EXE);
            } else {
                protocPath = Paths.get(protocPathProperty);
            }
            Path protocExe = makeExecutableFromPath(workDir, PROTOC_GROUPID, PROTOC, classifier, "exe", protocPath);

            Path protocGrpcPluginExe = prepareExecutable(workDir, model,
                    "io.grpc", "protoc-gen-grpc-java", classifier, "exe");

            Path quarkusGrpcPluginExe = prepareQuarkusGrpcExecutable(model, workDir);

            executables = new Executables(protocExe, protocGrpcPluginExe, quarkusGrpcPluginExe);
        }
    }

    private Path prepareExecutable(Path buildDir, ApplicationModel model,
            String groupId, String artifactId, String classifier, String packaging) throws CodeGenException {
        Path artifactPath = findArtifactPath(model, groupId, artifactId, classifier, packaging);

        return makeExecutableFromPath(buildDir, groupId, artifactId, classifier, packaging, artifactPath);
    }

    private Path makeExecutableFromPath(Path buildDir, String groupId, String artifactId, String classifier, String packaging,
            Path artifactPath) throws CodeGenException {
        Path exe = buildDir.resolve(String.format("%s-%s-%s-%s", groupId, artifactId, classifier, packaging));

        if (Files.exists(exe)) {
            return exe;
        }

        if (artifactPath == null) {
            String location = String.format("%s:%s:%s:%s", groupId, artifactId, classifier, packaging);
            throw new CodeGenException("Failed to find " + location + " among dependencies");
        }

        try {
            Files.copy(artifactPath, exe, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new CodeGenException("Failed to copy file: " + artifactPath + " to " + exe, e);
        }
        if (!exe.toFile().setExecutable(true)) {
            throw new CodeGenException("Failed to make the file executable: " + exe);
        }
        return exe;
    }

    private static Path findArtifactPath(ApplicationModel model, String groupId, String artifactId, String classifier,
            String packaging) {
        Path artifactPath = null;

        for (ResolvedDependency artifact : model.getDependencies()) {
            if (groupId.equals(artifact.getGroupId())
                    && artifactId.equals(artifact.getArtifactId())
                    && classifier.equals(artifact.getClassifier())
                    && packaging.equals(artifact.getType())) {
                artifactPath = artifact.getResolvedPaths().getSinglePath();
            }
        }
        return artifactPath;
    }

    private String osClassifier() throws CodeGenException {
        String architecture = OS.getArchitecture();
        switch (OS.determineOS()) {
            case LINUX:
                return "linux-" + architecture;
            case WINDOWS:
                return "windows-" + architecture;
            case MAC:
                return "osx-" + architecture;
            default:
                throw new CodeGenException(
                        "Unsupported OS, please use maven plugin instead to generate Java classes from proto files");
        }
    }

    private static Path prepareQuarkusGrpcExecutable(ApplicationModel appModel, Path buildDir) throws CodeGenException {
        Path pluginPath = findArtifactPath(appModel, "io.quarkus", "quarkus-grpc-protoc-plugin", "shaded", "jar");
        if (pluginPath == null) {
            throw new CodeGenException("Failed to find Quarkus gRPC protoc plugin among dependencies");
        }

        if (OS.determineOS() != OS.WINDOWS) {
            return writeScript(buildDir, pluginPath, "#!/bin/sh\n", ".sh");
        } else {
            return writeScript(buildDir, pluginPath, "@echo off\r\n", ".cmd");
        }
    }

    private static Path writeScript(Path buildDir, Path pluginPath, String shebang, String suffix) throws CodeGenException {
        Path script;
        try {
            script = Files.createTempFile(buildDir, "quarkus-grpc", suffix);
            try (BufferedWriter writer = Files.newBufferedWriter(script)) {
                writer.write(shebang);
                writePluginExeCmd(pluginPath, writer);
            }
        } catch (IOException e) {
            throw new CodeGenException("Failed to create a wrapper script for quarkus-grpc plugin", e);
        }
        if (!script.toFile().setExecutable(true)) {
            throw new CodeGenFailureException("failed to set file: " + script + " executable. Protoc invocation may fail");
        }
        return script;
    }

    private static void writePluginExeCmd(Path pluginPath, BufferedWriter writer) throws IOException {
        writer.write("\"" + JavaBinFinder.findBin() + "\" -cp \"" +
                pluginPath.toAbsolutePath() + "\" " + quarkusProtocPluginMain);
        writer.newLine();
    }

    private static class Executables {

        final Path protoc;
        final Path grpc;
        final Path quarkusGrpc;

        Executables(Path protoc, Path grpc, Path quarkusGrpc) {
            this.protoc = protoc;
            this.grpc = grpc;
            this.quarkusGrpc = quarkusGrpc;
        }
    }

    private static class GrpcCodeGenException extends RuntimeException {
        private GrpcCodeGenException(String message, Exception cause) {
            super(message, cause);
        }
    }
}
