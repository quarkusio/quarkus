package io.quarkus.dockerfiles.cli;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.dockerfiles.spi.GeneratedDockerfile;
import io.quarkus.maven.dependency.ArtifactDependency;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@TopCommand
@Command(name = "dockerfiles", sortOptions = false, mixinStandardHelpOptions = false, header = "Generate Dockerfiles.", headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", optionListHeading = "%nOptions:%n")
public class Dockerfiles implements Callable<Integer> {

    private static final ArtifactDependency QUARKUS_DOCKERFILES = new ArtifactDependency("io.quarkus", "quarkus-dockerfiles",
            null, "jar", Dockerfiles.getVersion());
    private static final ArtifactDependency QUARKUS_DOCKERFILES_SPI = new ArtifactDependency("io.quarkus",
            "quarkus-dockerfiles-spi", null, "jar", Dockerfiles.getVersion());

    @Option(names = { "--jvm" }, paramLabel = "", order = 5, description = "Flag to enable JVM Dockerfile generation")
    boolean generateJvmDockerfile;

    @Option(names = { "--native" }, paramLabel = "", order = 5, description = "Flag to enable Native Dockerfile generation")
    boolean generateNativeDockerfile;

    @Parameters(arity = "0..1", paramLabel = "GENERATION_PATH", description = " The path to generate Dockerfiles")
    Optional<String> generationPath;

    public Integer call() {
        Path projectRoot = getWorkingDirectory();
        BuildTool buildTool = QuarkusProjectHelper.detectExistingBuildTool(projectRoot);
        if (buildTool == null) {
            System.out.println("Unable to determine the build tool used for the project at " + projectRoot);
            return ExitCode.USAGE;
        }
        Path targetDirecotry = projectRoot.resolve(buildTool.getBuildDirectory());
        QuarkusBootstrap quarkusBootstrap = QuarkusBootstrap.builder()
                .setMode(QuarkusBootstrap.Mode.PROD)
                .setApplicationRoot(getWorkingDirectory())
                .setProjectRoot(getWorkingDirectory())
                .setTargetDirectory(targetDirecotry)
                .setLocalProjectDiscovery(true)
                .setIsolateDeployment(false)
                .setForcedDependencies(List.of(QUARKUS_DOCKERFILES, QUARKUS_DOCKERFILES_SPI))
                .setBaseClassLoader(ClassLoader.getSystemClassLoader())
                .build();

        List<String> resultBuildItemFQCNs = new ArrayList<>();

        boolean hasJvmSuffix = generationPath.map(p -> p.endsWith(".jvm")).orElse(false);
        boolean hasNativeSuffix = generationPath.map(p -> p.endsWith(".native")).orElse(false);
        boolean isDirectory = generationPath.map(p -> Paths.get(p).toFile().isDirectory())
                .orElse(Paths.get("").toFile().isDirectory());

        // Checking
        if (generateJvmDockerfile && hasNativeSuffix) {
            System.out.println("Cannot generate JVM Dockerfile when the path has a .native suffix");
            return ExitCode.USAGE;
        }
        if (generateNativeDockerfile && hasJvmSuffix) {
            System.out.println("Cannot generate Native Dockerfile when the path has a .jvm suffix");
            return ExitCode.USAGE;
        } else if (generateJvmDockerfile && generateNativeDockerfile && !isDirectory) {

        }

        if (generateJvmDockerfile || hasJvmSuffix) {
            resultBuildItemFQCNs.add(GeneratedDockerfile.Jvm.class.getName());
        }

        if (generateNativeDockerfile || hasNativeSuffix) {
            resultBuildItemFQCNs.add(GeneratedDockerfile.Native.class.getName());
        }

        if (resultBuildItemFQCNs.isEmpty()) {
            generateJvmDockerfile = true;
            resultBuildItemFQCNs.add(GeneratedDockerfile.Jvm.class.getName());
        }

        Path jvmDockerfile = (isDirectory
                ? generationPath.map(p -> Paths.get(p))
                : generationPath.map(Paths::get))
                .orElse(Paths.get("Dockerfile.jvm"));

        Path nativeDockerfile = (isDirectory
                ? generationPath.map(p -> Paths.get(p))
                : generationPath.map(Paths::get))
                .orElse(Paths.get("Dockerfile.native"));

        try (CuratedApplication curatedApplication = quarkusBootstrap.bootstrap()) {
            AugmentAction action = curatedApplication.createAugmentor();

            action.performCustomBuild(GenerateDockerfilesHandler.class.getName(), new Consumer<List<GeneratedDockerfile>>() {
                @Override
                public void accept(List<GeneratedDockerfile> dockerfiles) {
                    for (GeneratedDockerfile dockerfile : dockerfiles) {
                        if (dockerfile instanceof GeneratedDockerfile.Jvm) {
                            writeStringSafe(jvmDockerfile, dockerfile.getContent());
                            System.out.println("Generated JVM Dockerfile: " + jvmDockerfile);
                        } else if (dockerfile instanceof GeneratedDockerfile.Native) {
                            writeStringSafe(nativeDockerfile, dockerfile.getContent());
                            System.out.println("Generated Native Dockerfile: " + nativeDockerfile);
                        }
                    }
                }
            }, resultBuildItemFQCNs.toArray(new String[resultBuildItemFQCNs.size()]));

        } catch (BootstrapException e) {
            throw new RuntimeException(e);
        }
        return ExitCode.OK;
    }

    private void writeStringSafe(Path p, String content) {
        try {
            Files.writeString(p, content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Path getWorkingDirectory() {
        return Paths.get(System.getProperty("user.dir"));
    }

    private static String getVersion() {
        return read(Dockerfiles.class.getClassLoader().getResourceAsStream("version"));
    }

    private static String read(InputStream is) {
        try {
            return new String(is.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
