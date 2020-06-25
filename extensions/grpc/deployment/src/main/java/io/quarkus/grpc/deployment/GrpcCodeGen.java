package io.quarkus.grpc.deployment;

import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.prebuild.CodeGenException;
import io.quarkus.bootstrap.prebuild.CodeGenFailureException;
import io.quarkus.deployment.CodeGenContext;
import io.quarkus.deployment.CodeGenProvider;
import io.quarkus.utilities.JavaBinFinder;
import io.quarkus.utilities.OS;

public class GrpcCodeGen implements CodeGenProvider {
    private static final Logger log = Logger.getLogger(GrpcCodeGen.class);

    private static final String quarkusProtocPluginMain = "io.quarkus.grpc.protoc.plugin.MutinyGrpcGenerator";

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
                List<String> protoFiles = Files.walk(protoDir)
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".proto"))
                        .map(Path::toString)
                        .collect(Collectors.toList());
                if (!protoFiles.isEmpty()) {
                    initExecutables(workDir, context.appModel());

                    List<String> command = new ArrayList<>();
                    command.addAll(asList(executables.protoc.toString(),
                            "-I=" + protoDir.toString(),
                            "--plugin=protoc-gen-grpc=" + executables.grpc,
                            "--plugin=protoc-gen-q-grpc=" + executables.quarkusGrpc,
                            "--q-grpc_out=" + outDir,
                            "--grpc_out=" + outDir,
                            "--java_out=" + outDir));
                    command.addAll(protoFiles);

                    Process process = new ProcessBuilder()
                            .command(command)
                            .inheritIO()
                            .start();
                    int resultCode = process.waitFor();
                    if (resultCode != 0) {
                        throw new CodeGenException("Failed to generate Java classes from proto file: " + protoFiles);
                    }
                    return true;
                }
            }
        } catch (IOException | InterruptedException e) {
            throw new CodeGenException("Failed to generate java files from proto file in " + protoDir.toAbsolutePath(), e);
        }
        return false;
    }

    private void initExecutables(Path workDir, AppModel model) throws CodeGenException {
        if (executables == null) {
            Path protocExe = prepareExecutable(workDir, model,
                    "com.google.protobuf", "protoc", osClassifier(), "exe");
            Path protocGrpcPluginExe = prepareExecutable(workDir, model,
                    "io.grpc", "protoc-gen-grpc-java", osClassifier(), "exe");

            Path quarkusGrpcPluginExe = prepareQuarkusGrpcExecutable(model, workDir);

            executables = new Executables(protocExe, protocGrpcPluginExe, quarkusGrpcPluginExe);
        }
    }

    private Path prepareExecutable(Path buildDir, AppModel model,
            String groupId, String artifactId, String classifier, String packaging) throws CodeGenException {
        Path artifactPath = findArtifactPath(model, groupId, artifactId, classifier, packaging);

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
        Path pluginPath = findArtifactPath(appModel, "io.quarkus", "quarkus-grpc-protoc-plugin", "", "jar");
        if (pluginPath == null) {
            throw new CodeGenException("Failed to find Quarkus gRPC protoc plugin among dependencies");
        }

        if (OS.determineOS() != OS.WINDOWS) {
            return writeScript(buildDir, pluginPath, "#!/bin/sh\n", ".sh");
        } else {
            return writeScript(buildDir, pluginPath, "", ".cmd");
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
        writer.write(JavaBinFinder.findBin() + " -cp " +
                pluginPath.toAbsolutePath().toString() + " " + quarkusProtocPluginMain);
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
