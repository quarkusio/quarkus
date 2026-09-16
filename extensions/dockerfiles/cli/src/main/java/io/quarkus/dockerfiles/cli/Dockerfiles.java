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

import io.quarkus.bootstrap.BootstrapAppModelFactory;
import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.dockerfiles.spi.GeneratedDockerfile;
import io.quarkus.maven.dependency.ArtifactDependency;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@TopCommand
@Command(name = "dockerfiles", sortOptions = false, mixinStandardHelpOptions = false, header = "Generate Dockerfiles/Containerfiles.", headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", optionListHeading = "%nOptions:%n")
public class Dockerfiles implements Callable<Integer> {

    private static final ArtifactDependency QUARKUS_DOCKERFILES = new ArtifactDependency("io.quarkus", "quarkus-dockerfiles",
            null, "jar", Dockerfiles.getVersion());
    private static final ArtifactDependency QUARKUS_DOCKERFILES_SPI = new ArtifactDependency("io.quarkus",
            "quarkus-dockerfiles-spi", null, "jar", Dockerfiles.getVersion());

    @Option(names = {
            "--jvm" }, paramLabel = "", order = 5, description = "Flag to enable JVM Dockerfile generation. By default a JVM dockerfile is generated unless options imply native.")
    boolean generateJvmDockerfile;

    @Option(names = { "--native" }, paramLabel = "", order = 5, description = "Flag to enable Native Dockerfile generation")
    boolean generateNativeDockerfile;

    @Parameters(arity = "0..1", paramLabel = "GENERATION_PATH", description = "The path to generate Dockerfiles")
    Optional<String> generationPath;

    public List<Dependency> getProjectDependencies() {
        List<Dependency> dependencies = new ArrayList<>();
        dependencies.add(QUARKUS_DOCKERFILES);
        dependencies.add(QUARKUS_DOCKERFILES_SPI);
        try {
            BootstrapAppModelFactory.newInstance()
                    .setProjectRoot(getWorkingDirectory())
                    .setLocalProjectsDiscovery(true)
                    .resolveAppModel()
                    .getApplicationModel()
                    .getDependencies().forEach(d -> {
                        dependencies.add(new ArtifactDependency(d.getGroupId(), d.getArtifactId(), d.getClassifier(),
                                d.getType(), d.getVersion()));
                    });
        } catch (BootstrapException e) {
            //Ignore, as it's currently broken for gradle
        }
        return dependencies;
    }

    public Integer call() {
        Path projectRoot = getWorkingDirectory();
        BuildTool buildTool = QuarkusProjectHelper.detectExistingBuildTool(projectRoot);
        if (buildTool == null) {
            System.out.println("Unable to determine the build tool used for the project at " + projectRoot);
            return ExitCode.USAGE;
        }
        Path targetDirectory = projectRoot.resolve(buildTool.getBuildDirectory());
        QuarkusBootstrap quarkusBootstrap = QuarkusBootstrap.builder()
                .setMode(QuarkusBootstrap.Mode.PROD)
                .setApplicationRoot(getWorkingDirectory())
                .setProjectRoot(getWorkingDirectory())
                .setTargetDirectory(targetDirectory)
                .setLocalProjectDiscovery(true)
                .setIsolateDeployment(false)
                .setRebuild(true)
                .setForcedDependencies(getProjectDependencies())
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
            if (isArtifactResolutionError(e)) {
                System.err.println("Error: Project artifacts not found. Please build the project first:");
                System.err.println();
                if (buildTool == BuildTool.MAVEN) {
                    System.err.println("  ./mvnw clean package -DskipTests");
                } else if (buildTool == BuildTool.GRADLE) {
                    System.err.println("  ./gradlew build -x test");
                } else {
                    System.err.println("  Build your project using your build tool");
                }
                System.err.println();
                System.err.println("Then run the dockerfiles command again.");
                return ExitCode.USAGE;
            }

            System.err.println("Failed to bootstrap Quarkus application: " + e.getMessage());
            System.err.println("Run with --help for usage information.");
            return ExitCode.SOFTWARE;
        }
        return ExitCode.OK;
    }

    private boolean isArtifactResolutionError(BootstrapException e) {
        // Check if the error is related to missing artifacts or resolution issues
        String message = e.getMessage();
        Throwable cause = e.getCause();

        // Look for common patterns that indicate missing build artifacts
        if (message != null && (message.contains("Failed to create the application model") ||
                message.contains("Failed to resolve artifact") ||
                message.contains("Could not find artifact"))) {
            return true;
        }

        // Check nested causes for artifact resolution exceptions
        while (cause != null) {
            String causeMessage = cause.getMessage();
            if (causeMessage != null && (causeMessage.contains("Could not find artifact") ||
                    causeMessage.contains("ArtifactNotFoundException") ||
                    causeMessage.contains("Failed to resolve artifact"))) {
                return true;
            }
            cause = cause.getCause();
        }

        return false;
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
