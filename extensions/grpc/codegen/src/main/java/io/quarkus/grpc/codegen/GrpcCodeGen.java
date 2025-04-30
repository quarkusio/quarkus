package io.quarkus.grpc.codegen;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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
import io.quarkus.paths.PathFilter;
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
    private static final String SCAN_DEPENDENCIES_FOR_PROTO_INCLUDE_PATTERN = "quarkus.generate-code.grpc.scan-for-proto-include.\"%s\"";
    private static final String SCAN_DEPENDENCIES_FOR_PROTO_EXCLUDE_PATTERN = "quarkus.generate-code.grpc.scan-for-proto-exclude.\"%s\"";
    private static final String SCAN_FOR_IMPORTS = "quarkus.generate-code.grpc.scan-for-imports";

    private static final String POST_PROCESS_SKIP = "quarkus.generate.code.grpc-post-processing.skip";
    private static final String GENERATE_DESCRIPTOR_SET = "quarkus.generate-code.grpc.descriptor-set.generate";
    private static final String DESCRIPTOR_SET_OUTPUT_DIR = "quarkus.generate-code.grpc.descriptor-set.output-dir";
    private static final String DESCRIPTOR_SET_FILENAME = "quarkus.generate-code.grpc.descriptor-set.name";

    private static final String USE_ARG_FILE = "quarkus.generate-code.grpc.use-arg-file";

    private static final String GENERATE_KOTLIN = "quarkus.generate-code.grpc.kotlin.generate";

    private Executables executables;
    private String input;
    private boolean hasQuarkusKotlinDependency;

    @Override
    public String providerId() {
        return "grpc";
    }

    @Override
    public String[] inputExtensions() {
        return new String[] { "proto" };
    }

    @Override
    public String inputDirectory() {
        return "proto";
    }

    @Override
    public Path getInputDirectory() {
        if (input != null) {
            return Path.of(input);
        }
        return null;
    }

    @Override
    public void init(ApplicationModel model, Map<String, String> properties) {
        this.input = properties.get("quarkus.grpc.codegen.proto-directory");
        this.hasQuarkusKotlinDependency = containsQuarkusKotlin(model.getDependencies());
    }

    @Override
    public boolean trigger(CodeGenContext context) throws CodeGenException {
        if (TRUE.toString().equalsIgnoreCase(System.getProperties().getProperty("grpc.codegen.skip", "false"))
                || context.config().getOptionalValue("quarkus.grpc.codegen.skip", Boolean.class).orElse(false)) {
            log.info("Skipping gRPC code generation on user's request");
            return false;
        }
        Path outDir = context.outDir();
        Path workDir = context.workDir();
        Path inputDir = CodeGenProvider.resolve(context.inputDir());
        Set<String> protoDirs = new LinkedHashSet<>();

        boolean useArgFile = context.config().getOptionalValue(USE_ARG_FILE, Boolean.class).orElse(false);

        try {
            List<String> protoFiles = new ArrayList<>();
            if (Files.isDirectory(inputDir)) {
                try (Stream<Path> protoFilesPaths = Files.walk(inputDir)) {
                    protoFilesPaths
                            .filter(Files::isRegularFile)
                            .filter(s -> s.toString().endsWith(PROTO))
                            .map(Path::normalize)
                            .map(Path::toAbsolutePath)
                            .map(Path::toString)
                            .forEach(protoFiles::add);
                    protoDirs.add(inputDir.normalize().toAbsolutePath().toString());
                }
            }
            Path dirWithProtosFromDependencies = workDir.resolve("protoc-protos-from-dependencies");
            Collection<Path> protoFilesFromDependencies = gatherProtosFromDependencies(dirWithProtosFromDependencies, protoDirs,
                    context);
            if (!protoFilesFromDependencies.isEmpty()) {
                for (Path files : protoFilesFromDependencies) {
                    var pathToProtoFile = files.normalize().toAbsolutePath();
                    var pathToParentDir = files.getParent();
                    // Add the proto file to the list of proto to compile, but also add the directory containing the
                    // proto file to the list of directories to include (it's a set, so no duplicate).
                    protoFiles.add(pathToProtoFile.toString());
                    protoDirs.add(pathToParentDir.toString());

                }
            }

            if (!protoFiles.isEmpty()) {
                initExecutables(workDir, context.applicationModel());

                Collection<String> protosToImport = gatherDirectoriesWithImports(workDir.resolve("protoc-dependencies"),
                        context);

                List<String> command = new ArrayList<>();
                command.add(executables.protoc.toString());

                for (String protoDir : protoDirs) {
                    command.add(String.format("-I=%s", escapeWhitespace(protoDir)));
                }
                for (String protoImportDir : protosToImport) {
                    command.add(String.format("-I=%s", escapeWhitespace(protoImportDir)));
                }

                command.addAll(asList("--plugin=protoc-gen-grpc=" + executables.grpc,
                        "--plugin=protoc-gen-q-grpc=" + executables.quarkusGrpc,
                        "--q-grpc_out=" + outDir,
                        "--grpc_out=" + outDir,
                        "--java_out=" + outDir));

                if (shouldGenerateKotlin(context.config())) {
                    command.add("--kotlin_out=" + outDir);
                }

                if (shouldGenerateDescriptorSet(context.config())) {
                    command.add(String.format("--descriptor_set_out=%s", getDescriptorSetOutputFile(context)));
                }

                command.addAll(protoFiles);

                // Estimate command length to avoid command line too long error
                int commandLength = command.stream().mapToInt(String::length).sum();
                // 8191 is the maximum command line length for Windows
                if (useArgFile || (commandLength > 8190 && OS.determineOS() == OS.WINDOWS)) {
                    File argFile = File.createTempFile("grpc-protoc-params", ".txt");
                    argFile.deleteOnExit();

                    try (PrintWriter writer = new PrintWriter(argFile, StandardCharsets.UTF_8)) {
                        for (int i = 1; i < command.size(); i++) {
                            writer.println(command.get(i));
                        }
                    }

                    command = new ArrayList<>(Arrays.asList(command.get(0), "@" + argFile.getAbsolutePath()));
                }
                log.debugf("Executing command: %s", String.join(" ", command));
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
                    "Failed to generate java files from proto file in " + inputDir.toAbsolutePath(), e);
        }

        return false;
    }

    private static void copySanitizedProtoFile(ResolvedDependency artifact, Path protoPath, Path outProtoPath)
            throws IOException {
        boolean genericServicesFound = false;

        try (var reader = Files.newBufferedReader(protoPath);
                var writer = Files.newBufferedWriter(outProtoPath)) {

            String line = reader.readLine();
            while (line != null) {
                // filter java_generic_services to avoid "Tried to write the same file twice"
                // when set to true. Generic services are deprecated and replaced by classes generated by
                // this plugin
                if (!line.contains("java_generic_services")) {
                    writer.write(line);
                    writer.newLine();
                } else {
                    genericServicesFound = true;
                }

                line = reader.readLine();
            }
        }

        if (genericServicesFound) {
            log.infof("Ignoring option java_generic_services in %s:%s%s.", artifact.getGroupId(), artifact.getArtifactId(),
                    protoPath);
        }
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

        List<String> dependenciesToScan = Arrays.stream(scanDependencies.split(",")).map(String::trim)
                .collect(Collectors.toList());

        ApplicationModel appModel = context.applicationModel();
        List<Path> protoFilesFromDependencies = new ArrayList<>();
        for (ResolvedDependency artifact : appModel.getRuntimeDependencies()) {
            String packageId = String.format("%s:%s", artifact.getGroupId(), artifact.getArtifactId());
            Collection<String> includes = properties
                    .getOptionalValue(String.format(SCAN_DEPENDENCIES_FOR_PROTO_INCLUDE_PATTERN, packageId), String.class)
                    .map(s -> Arrays.stream(s.split(",")).map(String::trim).collect(Collectors.toList()))
                    .orElse(List.of());

            Collection<String> excludes = properties
                    .getOptionalValue(String.format(SCAN_DEPENDENCIES_FOR_PROTO_EXCLUDE_PATTERN, packageId), String.class)
                    .map(s -> Arrays.stream(s.split(",")).map(String::trim).collect(Collectors.toList()))
                    .orElse(List.of());

            if (scanAll
                    || dependenciesToScan.contains(packageId)) {
                extractProtosFromArtifact(workDir, protoFilesFromDependencies, protoDirectories, artifact, includes, excludes,
                        true);
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

    private boolean shouldGenerateKotlin(Config config) {
        return config.getOptionalValue(GENERATE_KOTLIN, Boolean.class).orElse(
                hasQuarkusKotlinDependency);
    }

    private boolean shouldGenerateDescriptorSet(Config config) {
        return config.getOptionalValue(GENERATE_DESCRIPTOR_SET, Boolean.class).orElse(FALSE);
    }

    private Path getDescriptorSetOutputFile(CodeGenContext context) throws IOException {
        var dscOutputDir = context.config().getOptionalValue(DESCRIPTOR_SET_OUTPUT_DIR, String.class)
                .map(context.workDir()::resolve)
                .orElseGet(context::outDir);

        if (Files.notExists(dscOutputDir)) {
            Files.createDirectories(dscOutputDir);
        }

        var dscFilename = context.config().getOptionalValue(DESCRIPTOR_SET_FILENAME, String.class)
                .orElse("descriptor_set.dsc");

        return dscOutputDir.resolve(dscFilename).normalize();
    }

    private Collection<String> gatherDirectoriesWithImports(Path workDir, CodeGenContext context) throws CodeGenException {
        Config properties = context.config();

        String scanForImports = properties.getOptionalValue(SCAN_FOR_IMPORTS, String.class)
                .orElse("com.google.protobuf:protobuf-java");

        if ("none".equals(scanForImports.toLowerCase(Locale.getDefault()))) {
            return Collections.emptyList();
        }

        boolean scanAll = "all".equals(scanForImports.toLowerCase(Locale.getDefault()));
        List<String> dependenciesToScan = Arrays.stream(scanForImports.split(",")).map(String::trim)
                .collect(Collectors.toList());

        Set<String> importDirectories = new HashSet<>();
        ApplicationModel appModel = context.applicationModel();
        for (ResolvedDependency artifact : appModel.getRuntimeDependencies()) {
            if (scanAll
                    || dependenciesToScan.contains(
                            String.format("%s:%s", artifact.getGroupId(), artifact.getArtifactId()))) {
                extractProtosFromArtifact(workDir, new ArrayList<>(), importDirectories, artifact, List.of(),
                        List.of(), false);
            }
        }
        return importDirectories;
    }

    private void extractProtosFromArtifact(Path workDir, Collection<Path> protoFiles,
            Set<String> protoDirectories, ResolvedDependency artifact, Collection<String> filesToInclude,
            Collection<String> filesToExclude, boolean isDependency) throws CodeGenException {

        try {
            artifact.getContentTree(new PathFilter(filesToInclude, filesToExclude)).walk(
                    pathVisit -> {
                        Path path = pathVisit.getPath();
                        if (Files.isRegularFile(path) && path.getFileName().toString().endsWith(PROTO)) {
                            Path root = pathVisit.getRoot();
                            if (Files.isDirectory(root)) {
                                protoFiles.add(path);
                                protoDirectories.add(path.getParent().normalize().toAbsolutePath().toString());
                            } else { // archive
                                Path relativePath = path.getRoot().relativize(path);
                                String uniqueName = artifact.getGroupId() + ":" + artifact.getArtifactId();
                                if (artifact.getVersion() != null) {
                                    uniqueName += ":" + artifact.getVersion();
                                }
                                if (artifact.getClassifier() != null) {
                                    uniqueName += "-" + artifact.getClassifier();
                                }
                                Path protoUnzipDir = workDir
                                        .resolve(HashUtil.sha1(uniqueName))
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
                                    if (isDependency) {
                                        copySanitizedProtoFile(artifact, path, outPath);
                                    } else {
                                        Files.copy(path, outPath, StandardCopyOption.REPLACE_EXISTING);
                                    }
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
            Path protocExe;
            if (protocPathProperty == null) {
                protocPath = findArtifactPath(model, PROTOC_GROUPID, PROTOC, classifier, EXE);
                protocExe = makeExecutableFromPath(workDir, PROTOC_GROUPID, PROTOC, classifier, "exe", protocPath);
            } else {
                log.debugf("Using protoc from %s", protocPathProperty);
                protocPath = Paths.get(protocPathProperty);
                protocExe = protocPath;
            }

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

    private static boolean containsQuarkusKotlin(Collection<ResolvedDependency> dependencies) {
        return dependencies.stream().anyMatch(new Predicate<ResolvedDependency>() {
            @Override
            public boolean test(ResolvedDependency rd) {
                return rd.getGroupId().equalsIgnoreCase("io.quarkus")
                        && rd.getArtifactId().equalsIgnoreCase("quarkus-kotlin");
            }
        });
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
