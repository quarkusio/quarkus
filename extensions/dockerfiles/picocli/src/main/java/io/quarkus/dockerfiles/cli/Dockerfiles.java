package io.quarkus.dockerfiles.cli;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.dockerfiles.spi.GeneratedJvmDockerfileBuildItem;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;

@Command(name = "dockerfiles", sortOptions = false, mixinStandardHelpOptions = false, header = "Generate Dockerfiles.", headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", optionListHeading = "%nOptions:%n")
public class Dockerfiles implements Callable<Integer> {

    public static void main(String[] args) {
        CommandLine.call(new Dockerfiles(), args);
    }

    public Integer call() {
        Path projectRoot = getWorkingDirectory();
        BuildTool buildTool = QuarkusProjectHelper.detectExistingBuildTool(projectRoot);
        if (buildTool == null) {
            System.out.println("Unable to determine the build tool used for the project at " + projectRoot);
            return ExitCode.USAGE;
        }
        Path targetDirecotry = projectRoot.resolve(buildTool.getBuildDirectory());
        System.out.println(
                "Detected project at: " + projectRoot.toAbsolutePath() + " with target:" + buildTool.getBuildDirectory());

        QuarkusBootstrap quarkusBootstrap = QuarkusBootstrap.builder()
                .setMode(QuarkusBootstrap.Mode.PROD)
                .setApplicationRoot(getWorkingDirectory())
                .setProjectRoot(getWorkingDirectory())
                .setTargetDirectory(targetDirecotry)
                .setLocalProjectDiscovery(true)
                .setIsolateDeployment(false)
                .build();

        List<String> resultBuildItemFQCNs = new ArrayList<>();
        resultBuildItemFQCNs.add(GeneratedJvmDockerfileBuildItem.class.getName());
        System.out.println("Generating Dockerfiles for the following build items: "
                + resultBuildItemFQCNs.stream().collect(Collectors.joining(", ")));

        try (CuratedApplication curatedApplication = quarkusBootstrap.bootstrap()) {
            AugmentAction action = curatedApplication.createAugmentor();
            AtomicReference<List<String>> tooMany = new AtomicReference<>();

            action.performCustomBuild(GenerateDockerfilesHandler.class.getName(), new Consumer<List<String>>() {
                @Override
                public void accept(List<String> strings) {
                    tooMany.set(strings);
                }
            }, resultBuildItemFQCNs.toArray(new String[resultBuildItemFQCNs.size()]));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ExitCode.OK;
    }

    private Path getWorkingDirectory() {
        return Paths.get(System.getProperty("user.dir"));
    }
}
