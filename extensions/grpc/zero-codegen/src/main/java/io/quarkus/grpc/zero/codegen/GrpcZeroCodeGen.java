package io.quarkus.grpc.zero.codegen;

import static io.roastedroot.protobuf4j.common.Protobuf.collectDependencies;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.nio.file.Files.copy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
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

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.compiler.PluginProtos;

import io.grpc.kotlin.generator.GeneratorRunner;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.prebuild.CodeGenException;
import io.quarkus.deployment.CodeGenContext;
import io.quarkus.deployment.CodeGenProvider;
import io.quarkus.grpc.protoc.plugin.MutinyGrpcGenerator;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathFilter;
import io.quarkus.runtime.util.HashUtil;
import io.roastedroot.protobuf4j.v4.Protobuf;
import io.roastedroot.zerofs.Configuration;
import io.roastedroot.zerofs.ZeroFs;
import io.smallrye.common.os.OS;

/**
 * Code generation for gRPC. Generates java classes from proto files placed in either src/main/proto or src/test/proto
 * Inspired by <a href="https://github.com/xolstice/protobuf-maven-plugin">Protobuf Maven Plugin</a>
 */
public class GrpcZeroCodeGen implements CodeGenProvider {
    private static final Logger log = Logger.getLogger(GrpcZeroCodeGen.class);

    private static final String PROTO = ".proto";

    private static final String SCAN_DEPENDENCIES_FOR_PROTO = "quarkus.generate-code.grpc.scan-for-proto";
    private static final String SCAN_DEPENDENCIES_FOR_PROTO_INCLUDE_PATTERN = "quarkus.generate-code.grpc.scan-for-proto-include.\"%s\"";
    private static final String SCAN_DEPENDENCIES_FOR_PROTO_EXCLUDE_PATTERN = "quarkus.generate-code.grpc.scan-for-proto-exclude.\"%s\"";
    private static final String SCAN_FOR_IMPORTS = "quarkus.generate-code.grpc.scan-for-imports";

    private static final String POST_PROCESS_SKIP = "quarkus.generate.code.grpc-post-processing.skip";
    private static final String GENERATE_DESCRIPTOR_SET = "quarkus.generate-code.grpc.descriptor-set.generate";
    private static final String DESCRIPTOR_SET_OUTPUT_DIR = "quarkus.generate-code.grpc.descriptor-set.output-dir";
    private static final String DESCRIPTOR_SET_FILENAME = "quarkus.generate-code.grpc.descriptor-set.name";

    private static final String GENERATE_KOTLIN = "quarkus.generate-code.grpc.kotlin.generate";

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
            log.info("Skipping gRPC Zero code generation on user's request");
            return false;
        }

        Path outDir = context.outDir();
        Path workDir = context.workDir();
        Path inputDir = CodeGenProvider.resolve(context.inputDir());
        Set<String> protoDirs = new LinkedHashSet<>();

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
            } catch (IOException e) {
                throw new CodeGenException("Failed to walk inputDir", e);
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
            Collection<String> protosToImport = gatherDirectoriesWithImports(workDir.resolve("protoc-dependencies"),
                    context);

            try (FileSystem fs = ZeroFs.newFileSystem(
                    Configuration.unix().toBuilder().setAttributeViews("unix").build())) {
                var workdir = fs.getPath(".");
                for (String protoDir : protoDirs) {
                    copyDirectory(Path.of(protoDir), workdir);
                }
                for (String protoImportDir : protosToImport) {
                    copyDirectory(Path.of(protoImportDir), workdir);
                }

                DescriptorProtos.FileDescriptorSet.Builder descriptorSetBuilder = DescriptorProtos.FileDescriptorSet
                        .newBuilder();
                PluginProtos.CodeGeneratorRequest.Builder requestBuilder = PluginProtos.CodeGeneratorRequest.newBuilder();

                var protobuf = Protobuf.builder().withWorkdir(workdir).build();

                for (String protoFile : protoFiles) {
                    try (InputStream is = Files.newInputStream(Path.of(protoFile))) {
                        Files.copy(is, workdir.resolve(Path.of(protoFile).getFileName().toString()),
                                StandardCopyOption.REPLACE_EXISTING);
                    }

                    log.info("resolving proto file: " + protoFile);
                    var protoName = realitivizeProtoFile(protoFile, protoDirs);
                    log.info("final proto name: " + protoName);

                    descriptorSetBuilder.addAllFile(protobuf.getDescriptors(List.of(protoName)).getFileList());
                    requestBuilder.addFileToGenerate(protoName);
                }

                // Load the previously generated descriptor
                DescriptorProtos.FileDescriptorSet descriptorSet = descriptorSetBuilder.build();

                // Add all FileDescriptorProto entries from the descriptor set
                // and all from dependencies
                resolveDependencies(protobuf, descriptorSet, requestBuilder);

                PluginProtos.CodeGeneratorRequest codeGeneratorRequest = requestBuilder.build();

                // protoc based plugins
                var javaResponse = Protobuf.runNativePlugin(
                        io.roastedroot.protobuf4j.common.Protobuf.NativePlugin.JAVA,
                        codeGeneratorRequest,
                        workdir);
                writeResultToDisk(javaResponse.getFileList(), outDir);
                var grpcJavaResponse = Protobuf.runNativePlugin(
                        io.roastedroot.protobuf4j.common.Protobuf.NativePlugin.GRPC_JAVA,
                        codeGeneratorRequest,
                        workdir);
                writeResultToDisk(grpcJavaResponse.getFileList(), outDir);

                log.info("Running MutinyGrpcGenerator plugin");
                List<PluginProtos.CodeGeneratorResponse.File> mutinyResponse = new MutinyGrpcGenerator()
                        .generateFiles(codeGeneratorRequest);

                writeResultToDisk(mutinyResponse, outDir);

                if (shouldGenerateKotlin(context.config())) {
                    log.info("Running KotlinGenerator plugin");
                    ByteArrayInputStream input = new ByteArrayInputStream(codeGeneratorRequest.toByteArray());
                    ByteArrayOutputStream output = new ByteArrayOutputStream();

                    GeneratorRunner.INSTANCE.mainAsProtocPlugin(input, output);

                    var response = PluginProtos.CodeGeneratorResponse.parseFrom(output.toByteArray());

                    writeResultToDisk(response.getFileList(), outDir);
                }

                if (shouldGenerateDescriptorSet(context.config())) {
                    Files.write(getDescriptorSetOutputFile(context), descriptorSet.toByteArray());
                }

                postprocessing(context, outDir);
                log.info("Grpc Zero: Successfully finished generating and post-processing sources from proto files");

                return true;
            } catch (IOException e) {
                throw new CodeGenException("Failed to generate files from proto file in " + inputDir.toAbsolutePath(), e);
            }
        }

        return false;
    }

    public static boolean isInSubtree(Path baseDir, Path candidate) {
        Path base = baseDir.toAbsolutePath().normalize();
        Path cand = candidate.toAbsolutePath().normalize();

        return cand.startsWith(base);
    }

    // TODO: verify is all this dance can be simplified somehow ...
    private static String realitivizeProtoFile(String protoFile, Set<String> protoDir) {
        Path protoFilePath = Path.of(protoFile);
        for (String dir : protoDir) {
            try {
                if (isInSubtree(Path.of(dir), protoFilePath)) {
                    Path base = Path.of(dir).toAbsolutePath().normalize();
                    Path file = protoFilePath.toAbsolutePath().normalize();
                    return base.relativize(file).toString();
                }
            } catch (IllegalArgumentException e) {
                // cannot be relativized, skip
            }
        }
        return protoFilePath.getFileName().toString();
    }

    private static void writeResultToDisk(List<PluginProtos.CodeGeneratorResponse.File> responseFileList, Path outDir)
            throws IOException {
        for (PluginProtos.CodeGeneratorResponse.File file : responseFileList) {
            Path outputPath = outDir.resolve(file.getName());
            // TODO: add a check when hitting root?
            Files.createDirectories(outputPath.getParent());
            log.info("grpc file generated: " + outputPath);
            Files.writeString(outputPath, file.getContent());
        }
    }

    private static void resolveDependencies(Protobuf protobuf,
            DescriptorProtos.FileDescriptorSet descriptorSet, PluginProtos.CodeGeneratorRequest.Builder requestBuilder)
            throws CodeGenException {
        // Use protobuf4j's buildFileDescriptors to resolve all dependencies
        List<Descriptors.FileDescriptor> fileDescriptors = protobuf.buildFileDescriptors(descriptorSet);

        // Collect all file descriptors (including transitive dependencies) into a new descriptor set
        DescriptorProtos.FileDescriptorSet.Builder allDescriptorsBuilder = DescriptorProtos.FileDescriptorSet.newBuilder();
        Set<String> added = new HashSet<>();

        // For each file descriptor in the original set, collect all its dependencies
        for (Descriptors.FileDescriptor fileDescriptor : fileDescriptors) {
            collectDependencies(fileDescriptor, allDescriptorsBuilder, added);
        }

        // Add all collected FileDescriptorProto objects to the request builder
        DescriptorProtos.FileDescriptorSet allDescriptors = allDescriptorsBuilder.build();
        requestBuilder.addAllProtoFile(allDescriptors.getFileList());
    }

    /**
     * Normalizes a relative path by removing leading "./" or "." which can cause issues on Windows with ZeroFS.
     *
     * @param source the source directory path
     * @param path the file or directory path to relativize
     * @return the normalized relative path string
     */
    private static String normalizeRelativePath(Path source, Path path) {
        String relative = source.relativize(path).toString().replace("\\", "/");
        // Normalize relative path to remove leading "./" which can cause issues on Windows with ZeroFS
        if (relative.startsWith("./")) {
            relative = relative.substring(2);
        } else if (relative.startsWith(".")) {
            relative = relative.substring(1);
        }
        return relative;
    }

    public static void copyDirectory(final Path source, final Path target) throws IOException {
        java.nio.file.Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (java.nio.file.Files.isSymbolicLink(dir)) {
                    return FileVisitResult.SKIP_SUBTREE;
                } else {
                    String relative = normalizeRelativePath(source, dir);
                    Path directory = target.resolve(relative);
                    if (!directory.toString().equals("/")) {
                        FileAttribute<?>[] attributes = new FileAttribute[0];
                        PosixFileAttributeView attributeView = (PosixFileAttributeView) java.nio.file.Files
                                .getFileAttributeView(dir, PosixFileAttributeView.class);
                        if (attributeView != null) {
                            Set<PosixFilePermission> permissions = attributeView.readAttributes().permissions();
                            FileAttribute<Set<PosixFilePermission>> attribute = PosixFilePermissions
                                    .asFileAttribute(permissions);
                            attributes = new FileAttribute[] { attribute };
                        }

                        java.nio.file.Files.createDirectories(directory, attributes);
                    }

                    return FileVisitResult.CONTINUE;
                }
            }

            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String relative = normalizeRelativePath(source, file);
                Path path = target.resolve(relative);
                // Ensure parent directory exists before copying
                if (path.getParent() != null) {
                    java.nio.file.Files.createDirectories(path.getParent());
                }
                java.nio.file.Files.copy(file, path, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
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

        new GrpcZeroPostProcessing(context, outDir).postprocess();

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
                                        copy(path, outPath, StandardCopyOption.REPLACE_EXISTING);
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
        if (OS.current() == OS.LINUX) {
            return path.replace(" ", "\\ ");
        } else {
            return path;
        }
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

    private static class GrpcCodeGenException extends RuntimeException {
        private GrpcCodeGenException(String message, Exception cause) {
            super(message, cause);
        }
    }
}
