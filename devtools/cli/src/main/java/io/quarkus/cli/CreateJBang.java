package io.quarkus.cli;

import java.io.File;
import java.util.Set;
import java.util.concurrent.Callable;

import io.quarkus.cli.core.BaseSubCommand;
import io.quarkus.devtools.commands.CreateJBangProject;
import io.quarkus.platform.tools.config.QuarkusPlatformConfig;
import picocli.CommandLine;

@CommandLine.Command(name = "create-jbang", sortOptions = false, usageHelpAutoWidth = true, mixinStandardHelpOptions = false, description = "Create a new quarkus jbang project.")
public class CreateJBang extends BaseSubCommand implements Callable<Integer> {

    @CommandLine.Option(names = { "-n",
            "--no-wrapper" }, order = 1, description = "Generate without JBang wrapper.")
    boolean noJBangWrapper = false;

    @CommandLine.Option(names = { "-o",
            "--output-folder" }, order = 2, paramLabel = "OUTPUT-FOLDER", description = "The output folder for project")
    String outputFolder = "jbang-with-quarkus";

    @CommandLine.Parameters(arity = "0..1", paramLabel = "EXTENSION", description = "Extensions to add to project")
    Set<String> extensions;

    @Override
    public Integer call() throws Exception {
        try {
            File projectDirectory = new File(System.getProperty("user.dir"));

            File projectRoot = new File(projectDirectory.getAbsoluteFile(), outputFolder);
            if (projectRoot.exists()) {
                err().println("Unable to create the project, " +
                        "the directory " + projectRoot.getAbsolutePath() + " already exists");
                return CommandLine.ExitCode.SOFTWARE;
            }

            boolean status = new CreateJBangProject(projectRoot.getAbsoluteFile().toPath(),
                    QuarkusPlatformConfig.getGlobalDefault().getPlatformDescriptor())
                            .extensions(extensions)
                            .setValue("noJBangWrapper", noJBangWrapper)
                            .execute()
                            .isSuccess();

            if (status) {
                out().println("JBang project created.");
                parent.setProjectDirectory(projectRoot.toPath().toAbsolutePath());
            } else {
                err().println("Failed to create JBang project");
                return CommandLine.ExitCode.SOFTWARE;
            }
        } catch (Exception e) {
            if (parent.showErrors)
                e.printStackTrace(err());
            err().println("JBang project creation failed, " + e.getMessage());
            return CommandLine.ExitCode.SOFTWARE;
        }
        return CommandLine.ExitCode.OK;
    }
}
