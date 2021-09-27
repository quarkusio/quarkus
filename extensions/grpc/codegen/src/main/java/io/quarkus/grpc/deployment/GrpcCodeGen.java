package io.quarkus.grpc.deployment;

import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static org.codehaus.plexus.util.FileUtils.copyStreamToFile;

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
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.codehaus.plexus.util.io.RawInputStreamFacade;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.prebuild.CodeGenException;
import io.quarkus.bootstrap.prebuild.CodeGenFailureException;
import io.quarkus.deployment.CodeGenContext;
import io.quarkus.deployment.CodeGenProvider;
import io.quarkus.deployment.util.ProcessUtil;
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
        if (TRUE.toString().equalsIgnoreCase(System.getProperties().getProperty("grpc.codegen.skip", "false"))) {
            log.info("Skipping " + this.getClass() + " invocation on user's request");
            return false;
        }
        Path outDir = context.outDir();
        Path workDir = context.workDir();
        Path protoDir = context.inputDir();

        try {
            if (Files.isDirectory(protoDir)) {
                try (Stream<Path> protoFilesPaths = Files.walk(protoDir)) {
                    List<String> protoFiles = protoFilesPaths
                            .filter(Files::isRegularFile)
                            .map(Path::toString)
                            .filter(s -> s.endsWith(PROTO))
                            .map(this::escapeWhitespace)
                            .collect(Collectors.toList());
                    if (!protoFiles.isEmpty()) {
                        initExecutables(workDir, context.appModel());

                        Collection<String> protosToImport = gatherImports(workDir.resolve("protoc-dependencies"), context);

                        List<String> command = new ArrayList<>();
                        command.add(executables.protoc.toString());
                        for (String protoImportDir : protosToImport) {
                            command.add(String.format("-I=%s", escapeWhitespace(protoImportDir)));
                        }

                        command.addAll(asList("-I=" + escapeWhitespace(protoDir.toString()),
                                "--plugin=protoc-gen-grpc=" + executables.grpc,
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
                                    " to " + outDir.toAbsolutePath());
                        }
                        return true;
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            throw new CodeGenException("Failed to generate java files from proto file in " + protoDir.toAbsolutePath(), e);
        }
        return false;
    }

    private Collection<String> gatherImports(Path workDir, CodeGenContext context) throws CodeGenException {
        Map<String, String> properties = context.properties();

        String scanForImports = properties.getOrDefault("quarkus.generate-code.grpc.scan-for-imports",
                "com.google.protobuf:protobuf-java");

        if ("none".equals(scanForImports.toLowerCase(Locale.getDefault()))) {
            return Collections.emptyList();
        }

        boolean scanAll = "all".equals(scanForImports.toLowerCase(Locale.getDefault()));
        List<String> dependenciesToScan = Arrays.asList(scanForImports.split(","));

        Set<String> importDirectories = new HashSet<>();
        AppModel appModel = context.appModel();
        for (AppDependency dependency : appModel.getUserDependencies()) {
            AppArtifact artifact = dependency.getArtifact();
            if (scanAll
                    || dependenciesToScan.contains(String.format("%s:%s", artifact.getGroupId(), artifact.getArtifactId()))) {
                for (Path path : artifact.getPaths()) {
                    Path jarName = path.getFileName();
                    if (jarName.toString().endsWith(".jar")) {
                        final JarFile jar;
                        try {
                            jar = new JarFile(path.toFile());
                        } catch (final IOException e) {
                            throw new CodeGenException("Failed to read Jar: " + path.normalize().toAbsolutePath(), e);
                        }

                        for (JarEntry jarEntry : jar.stream().collect(Collectors.toList())) {
                            String jarEntryName = jarEntry.getName();
                            if (jarEntryName.endsWith(PROTO)) {
                                Path protoUnzipDir = workDir.resolve(HashUtil.sha1(jar.getName())).normalize().toAbsolutePath();
                                try {
                                    Files.createDirectories(protoUnzipDir);
                                    importDirectories.add(protoUnzipDir.toString());
                                } catch (IOException e) {
                                    throw new CodeGenException(
                                            "Failed to create directory: " + protoUnzipDir, e);
                                }
                                // checking for https://snyk.io/research/zip-slip-vulnerability
                                Path unzippedProto = protoUnzipDir.resolve(jarEntryName);
                                if (!unzippedProto.normalize().toAbsolutePath().startsWith(workDir.toAbsolutePath())) {
                                    throw new CodeGenException("Attempted to unzip " + jarEntryName
                                            + " to a location outside the working directory");
                                }
                                try {
                                    copyStreamToFile(new RawInputStreamFacade(jar.getInputStream(jarEntry)),
                                            unzippedProto.toFile());
                                } catch (IOException e) {
                                    throw new CodeGenException("Failed to create input stream for reading " + jarEntryName
                                            + " from " + jar.getName(), e);
                                }
                            }
                        }
                    }
                }
            }
        }
        return importDirectories;
    }

    private String escapeWhitespace(String path) {
        if (OS.determineOS() == OS.LINUX) {
            return path.replace(" ", "\\ ");
        } else {
            return path;
        }
    }

    private void initExecutables(Path workDir, AppModel model) throws CodeGenException {
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

    private Path prepareExecutable(Path buildDir, AppModel model,
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

    private static Path findArtifactPath(AppModel model, String groupId, String artifactId, String classifier,
            String packaging) {
        Path artifactPath = null;

        for (AppDependency dep : model.getFullDeploymentDeps()) {
            AppArtifact artifact = dep.getArtifact();
            if (groupId.equals(artifact.getGroupId())
                    && artifactId.equals(artifact.getArtifactId())
                    && classifier.equals(artifact.getClassifier())
                    && packaging.equals(artifact.getType())) {
                artifactPath = artifact.getPaths().getSinglePath();
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

    private static Path prepareQuarkusGrpcExecutable(AppModel appModel, Path buildDir) throws CodeGenException {
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
}
