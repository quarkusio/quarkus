package io.quarkus.cli.core;

import static picocli.CommandLine.ExitCode.OK;
import static picocli.CommandLine.ExitCode.SOFTWARE;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import io.quarkus.cli.QuarkusCli;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.registry.config.RegistriesConfigLocator;
import io.quarkus.utilities.OS;
import picocli.CommandLine;

public class ExecuteUtil {

    private static int executeGradle(File projectDirectory, QuarkusCli cli, String... args)
            throws Exception {
        File gradle = findExecutableFile("gradle");

        if (gradle == null) {
            cli.err().println("Unable to find the gradle executable, is it in your path?");
            return SOFTWARE;
        } else {
            String[] newArgs = prependArray(gradle.getAbsolutePath(), args);
            return executeProcess(cli, newArgs, projectDirectory);
        }
    }

    public static String[] prependArray(String prepend, String[] args) {
        String[] newArgs = new String[1 + args.length];
        newArgs[0] = prepend;
        for (int i = 1; i < newArgs.length; i++)
            newArgs[i] = args[i - 1];
        return newArgs;
    }

    public static File findExecutableFile(String base) {
        String path = null;
        String executable = base;

        if (OS.determineOS() == OS.WINDOWS) {
            executable = base + ".cmd";
            ;
            path = findExecutable(executable);
            if (path == null) {
                executable = base + ".bat";
                path = findExecutable(executable);
            }
        } else {
            executable = base;
            path = findExecutable(executable);
        }
        if (path == null)
            return null;
        return new File(path, executable);
    }

    private static int executeMaven(File projectDirectory, QuarkusCli cli, String... args) throws Exception {
        File mvn = findExecutableFile("mvn");
        if (mvn == null) {
            cli.err().println("Unable to find the maven executable, is it in your path?");
            return CommandLine.ExitCode.SOFTWARE;
        }

        String[] newArgs = prependArray(mvn.getAbsolutePath(), args);
        return executeProcess(cli, newArgs, projectDirectory);
    }

    private static String findExecutable(String exec) {
        return Stream.of(System.getenv("PATH").split(Pattern.quote(File.pathSeparator)))
                .map(Paths::get).map(path -> path.resolve(exec).toFile())
                .filter(File::exists).findFirst()
                .map(File::getParent).orElse(null);
    }

    private static int executeWrapper(File wrapper, QuarkusCli cli, String... args) throws Exception {
        String[] newArgs = prependArray(wrapper.getAbsolutePath(), args);

        File parentDir = wrapper.getParentFile();
        return executeProcess(cli, newArgs, parentDir);
    }

    public static int executeProcess(QuarkusCli cli, String[] args, File parentDir) throws IOException, InterruptedException {
        if (cli.isVerbose()) {
            cli.out().println(String.join(" ", args));
            cli.out().println();
        }

        int exit = SOFTWARE;
        if (cli.isManualOutput()) {
            // manual output is for unit testing the cli
            Process process = new ProcessBuilder()
                    .command(args)
                    .redirectErrorStream(true)
                    .directory(parentDir)
                    .start();
            InputStream stdIn = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(stdIn);
            BufferedReader br = new BufferedReader(isr);

            String line = null;

            while ((line = br.readLine()) != null)
                cli.out().println(line);

            exit = process.waitFor();

        } else {
            Process process = new ProcessBuilder()
                    .command(args)
                    .inheritIO()
                    .directory(parentDir)
                    .start();

            exit = process.waitFor();
        }
        if (exit != 0) {
            return SOFTWARE;
        } else {
            return OK;
        }
    }

    private static File getGradleWrapper(String projectPath) {
        if (OS.determineOS() == OS.WINDOWS) {
            File wrapper = new File(projectPath + File.separator + "gradlew.cmd");
            if (wrapper.isFile())
                return wrapper;
            wrapper = new File(projectPath + File.separator + "gradlew.bat");
            if (wrapper.isFile())
                return wrapper;
        } else {
            File wrapper = new File(projectPath + File.separator + "gradlew");
            if (wrapper.isFile())
                return wrapper;
        }

        return null;
    }

    private static File getMavenWrapper(String projectPath) {
        if (OS.determineOS() == OS.WINDOWS) {
            File wrapper = new File(projectPath + File.separator + "mvnw.cmd");
            if (wrapper.isFile())
                return wrapper;
            wrapper = new File(projectPath + File.separator + "mvnw.bat");
            if (wrapper.isFile())
                return wrapper;
        } else {
            File wrapper = new File(projectPath + File.separator + "mvnw");
            if (wrapper.isFile())
                return wrapper;
        }

        return null;
    }

    public static int executeGradleTarget(File projectPath, QuarkusCli cli, String... args) throws Exception {
        File buildFile = projectPath.toPath().resolve("build.gradle").toFile();
        if (!buildFile.isFile()) {
            cli.err().println("Was not able to find a build file in: " + projectPath);
            return SOFTWARE;
        }
        String[] newArgs = args;
        newArgs = propagatePropertyIfSet("maven.repo.local", newArgs);
        newArgs = propagatePropertyIfSet(RegistriesConfigLocator.CONFIG_FILE_PATH_PROPERTY, newArgs);
        newArgs = propagatePropertyIfSet("io.quarkus.maven.secondary-local-repo", newArgs);
        if (cli.isShowErrors()) {
            newArgs = prependArray("--full-stacktrace", newArgs);
        }
        if (CommandLine.Help.Ansi.AUTO.enabled())
            newArgs = prependArray("--console=rich", newArgs);
        File wrapper = getGradleWrapper(projectPath.getAbsolutePath());
        if (wrapper != null) {
            return executeWrapper(wrapper, cli, newArgs);
        } else {
            return executeGradle(projectPath, cli, args);
        }
    }

    private static String[] propagatePropertyIfSet(String name, String[] newArgs) {
        final String value = System.getProperty(name);
        return value == null ? newArgs : prependArray("-D" + name + "=" + value, newArgs);
    }

    public static int executeMavenTarget(File projectPath, QuarkusCli cli, String... args) throws Exception {
        File buildFile = projectPath.toPath().resolve("pom.xml").toFile();
        if (!buildFile.isFile()) {
            cli.err().println("Was not able to find a build file in: " + projectPath);
            return SOFTWARE;
        }
        File wrapper = getMavenWrapper(projectPath.getAbsolutePath());
        String[] newArgs = args;
        if (cli.isShowErrors()) {
            newArgs = prependArray("-e", newArgs);
        }
        if (CommandLine.Help.Ansi.AUTO.enabled())
            newArgs = prependArray("-Dstyle.color=always", newArgs);
        if (wrapper != null) {
            executeWrapper(wrapper, cli, newArgs);
        } else {
            return executeMaven(projectPath, cli, newArgs);
        }
        return CommandLine.ExitCode.OK;
    }

    public static void outputBuildCommand(PrintWriter writer, BuildTool buildtool, java.util.List<String> args) {
        writer.print(buildtool == BuildTool.MAVEN ? "mvn" : "gradle");
        for (String arg : args)
            writer.print(" " + arg);
    }
}
