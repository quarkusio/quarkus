package io.quarkus.cli;

import static io.quarkus.cli.build.MavenRunner.MAVEN_SETTINGS;
import static org.apache.maven.cli.MavenCli.LOCAL_REPO_PROPERTY;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Assertions;

import picocli.CommandLine;

public class CliDriver {
    static final PrintStream stdout = System.out;
    static final PrintStream stderr = System.err;
    private static final BinaryOperator<String> ARG_FORMATTER = (key, value) -> "-D" + key + "=" + value;
    private static final UnaryOperator<String> REPO_ARG_FORMATTER = value -> ARG_FORMATTER.apply(LOCAL_REPO_PROPERTY, value);
    private static final UnaryOperator<String> SETTINGS_ARG_FORMATTER = value -> ARG_FORMATTER.apply(MAVEN_SETTINGS, value);

    public static class CliDriverBuilder {

        private Path startingDir;
        private List<String> args = new ArrayList<>();
        private String mavenLocalRepo;
        private String mavenSettings;

        private CliDriverBuilder() {
        }

        public CliDriverBuilder setStartingDir(Path startingDir) {
            this.startingDir = startingDir;
            return this;
        }

        public CliDriverBuilder addArgs(String... args) {
            for (String s : args) {
                this.args.add(s);
            }
            return this;
        }

        public CliDriverBuilder setMavenRepoLocal(String mavenRepoLocal) {
            this.mavenLocalRepo = mavenRepoLocal;
            return this;
        }

        public CliDriverBuilder setMavenSettings(String mavenSettings) {
            this.mavenSettings = mavenSettings;
            return this;
        }

        public Result execute() throws Exception {
            List<String> newArgs = args;

            List<String> looseArgs = Collections.emptyList();
            int index = newArgs.indexOf("--");
            if (index >= 0) {
                looseArgs = new ArrayList<>(newArgs.subList(index, newArgs.size()));
                newArgs.subList(index, newArgs.size()).clear();
            }

            Optional.ofNullable(mavenLocalRepo).or(CliDriver::getMavenLocalRepoProperty).map(REPO_ARG_FORMATTER)
                    .ifPresent(newArgs::add);
            Optional.ofNullable(mavenSettings).or(CliDriver::getMavenSettingsProperty).map(SETTINGS_ARG_FORMATTER)
                    .ifPresent(newArgs::add);

            newArgs.add("--cli-test");
            newArgs.add("--cli-test-dir");
            newArgs.add(startingDir.toString());
            newArgs.addAll(looseArgs); // re-add arguments

            System.out.println("$ quarkus " + String.join(" ", newArgs));

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintStream outPs = new PrintStream(out);
            System.setOut(outPs);

            ByteArrayOutputStream err = new ByteArrayOutputStream();
            PrintStream errPs = new PrintStream(err);
            System.setErr(errPs);

            final Map<String, String> originalProps = collectOverriddenProps(newArgs);

            Result result = new Result();
            QuarkusCli cli = new QuarkusCli();
            try {
                result.exitCode = cli.run(newArgs.toArray(String[]::new));
                outPs.flush();
                errPs.flush();
            } finally {
                System.setOut(stdout);
                System.setErr(stderr);
                resetProperties(originalProps);
            }
            result.stdout = out.toString();
            result.stderr = err.toString();
            return result;
        }

        protected void resetProperties(Map<String, String> originalProps) {
            for (Map.Entry<String, String> origProp : originalProps.entrySet()) {
                if (origProp.getValue() == null) {
                    System.clearProperty(origProp.getKey());
                } else {
                    System.setProperty(origProp.getKey(), origProp.getValue());
                }
            }
        }

        protected Map<String, String> collectOverriddenProps(List<String> newArgs) {
            final Map<String, String> originalProps = new HashMap<>();
            for (String s : newArgs) {
                if (s.startsWith("-D")) {
                    int equals = s.indexOf('=', 2);
                    if (equals > 0) {
                        final String propName = s.substring(2, equals);
                        final String origValue = System.getProperty(propName);
                        if (origValue != null) {
                            originalProps.put(propName, origValue);
                        } else if (System.getProperties().contains(propName)) {
                            originalProps.put(propName, "true");
                        } else {
                            originalProps.put(propName, null);
                        }
                    }
                }
            }
            return originalProps;
        }
    }

    public static CliDriverBuilder builder() {
        return new CliDriverBuilder();
    }

    public static void preserveLocalRepoSettings(Collection<String> args) {
        getMavenLocalRepoProperty().map(REPO_ARG_FORMATTER).ifPresent(args::add);
        getMavenSettingsProperty().map(SETTINGS_ARG_FORMATTER).ifPresent(args::add);
    }

    public static Result executeArbitraryCommand(Path startingDir, String... args) throws Exception {
        System.out.println("$ " + String.join(" ", args));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream outPs = new PrintStream(out);
        System.setOut(outPs);

        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PrintStream errPs = new PrintStream(err);
        System.setErr(errPs);

        Result result = new Result();
        try {
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.directory(startingDir.toFile());
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);

            Process p = pb.start();
            p.waitFor();
            outPs.flush();
            errPs.flush();
        } finally {
            System.setOut(stdout);
            System.setErr(stderr);
        }
        result.stdout = out.toString();
        result.stderr = err.toString();
        return result;
    }

    public static Result execute(Path startingDir, String... args) throws Exception {
        return builder().setStartingDir(startingDir).addArgs(args).execute();
    }

    public static void println(String msg) {
        System.out.println(msg);
    }

    public static class Result {
        int exitCode;
        String stdout;
        String stderr;

        public int getExitCode() {
            return exitCode;
        }

        public String getStdout() {
            return stdout;
        }

        public void setStdout(String stdout) {
            this.stdout = stdout;
        }

        public String getStderr() {
            return stderr;
        }

        public void setStderr(String stderr) {
            this.stderr = stderr;
        }

        public void setExitCode(int exitCode) {
            this.exitCode = exitCode;
        }

        public void echoSystemOut() {
            System.out.println(stdout);
            System.out.println();
        }

        public void echoSystemErr() {
            System.out.println(stderr);
            System.out.println();
        }

        @Override
        public String toString() {
            return "result: {\n  exitCode: {" + exitCode
                    + "},\n  system_err: {" + stderr
                    + "},\n  system_out: {" + stdout + "}\n}";
        }
    }

    public static void deleteDir(Path path) throws Exception {
        if (!path.toFile().exists()) {
            return;
        }

        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(f -> retryDelete(f));

        Assertions.assertFalse(path.toFile().exists());
    }

    public static String readFileAsString(Path path) throws Exception {
        return new String(Files.readAllBytes(path));
    }

    public static void valdiateGeneratedSourcePackage(Path projectRoot, String name) {
        Path packagePath = projectRoot.resolve("src/main/java/" + name);
        Assertions.assertTrue(packagePath.toFile().exists(),
                "Package directory should exist: " + packagePath.toAbsolutePath().toString());
        Assertions.assertTrue(packagePath.toFile().isDirectory(),
                "Package directory should be a directory: " + packagePath.toAbsolutePath().toString());
    }

    public static void valdiateGeneratedTestPackage(Path projectRoot, String name) {
        Path packagePath = projectRoot.resolve("src/test/java/" + name);
        Assertions.assertTrue(packagePath.toFile().exists(),
                "Package directory should exist: " + packagePath.toAbsolutePath().toString());
        Assertions.assertTrue(packagePath.toFile().isDirectory(),
                "Package directory should be a directory: " + packagePath.toAbsolutePath().toString());
    }

    public static Result invokeValidateExtensionList(Path projectRoot) throws Exception {
        Result result = execute(projectRoot, "extension", "list", "-e", "-B", "--verbose");

        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);

        Assertions.assertFalse(result.stdout.contains("camel-"),
                "camel extensions should not appear in the list of installable extensions. Found:\n" + result);

        return result;
    }

    public static Result invokeExtensionAddQute(Path projectRoot, Path file) throws Exception {
        // add the qute extension
        Result result = execute(projectRoot, "extension", "add", "qute", "-e", "-B", "--verbose");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);

        // list all extensions, make sure qute is present
        result = invokeValidateExtensionList(projectRoot);
        Assertions.assertTrue(result.stdout.contains("quarkus-qute"),
                "Expected quarkus-qute to be in the list of extensions. Result:\n" + result);

        String content = readFileAsString(file);
        Assertions.assertTrue(content.contains("quarkus-qute"),
                "quarkus-qute should be listed as a dependency. Result:\n" + content);

        return result;
    }

    public static Result invokeExtensionRemoveQute(Path projectRoot, Path file) throws Exception {
        // remove the qute extension
        Result result = execute(projectRoot, "extension", "remove", "qute", "-e", "-B", "--verbose");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);

        // list all extensions, make sure qute is present
        result = invokeValidateExtensionList(projectRoot);
        Assertions.assertFalse(result.stdout.contains("quarkus-qute"),
                "Expected quarkus-qute to be missing from the list of extensions. Result:\n" + result);

        String content = readFileAsString(file);
        Assertions.assertFalse(content.contains("quarkus-qute"),
                "quarkus-qute should not be listed as a dependency. Result:\n" + content);

        return result;
    }

    public static Result invokeExtensionAddMultiple(Path projectRoot, Path file) throws Exception {
        // add the qute extension
        Result result = execute(projectRoot, "extension", "add", "amazon-lambda-http", "jackson", "-e", "-B", "--verbose");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);

        // list all extensions, make sure all are present
        result = invokeValidateExtensionList(projectRoot);
        Assertions.assertTrue(result.stdout.contains("quarkus-qute"),
                "Expected quarkus-qute to be in the list of extensions. Result:\n" + result);
        Assertions.assertTrue(result.stdout.contains("quarkus-amazon-lambda-http"),
                "Expected quarkus-amazon-lambda-http to be in the list of extensions. Result:\n" + result);
        Assertions.assertTrue(result.stdout.contains("quarkus-jackson"),
                "Expected quarkus-jackson to be in the list of extensions. Result:\n" + result);

        String content = CliDriver.readFileAsString(file);
        Assertions.assertTrue(content.contains("quarkus-qute"),
                "quarkus-qute should still be listed as a dependency. Result:\n" + content);
        Assertions.assertTrue(content.contains("quarkus-amazon-lambda-http"),
                "quarkus-amazon-lambda-http should be listed as a dependency. Result:\n" + content);
        Assertions.assertTrue(content.contains("quarkus-jackson"),
                "quarkus-jackson should be listed as a dependency. Result:\n" + content);

        return result;
    }

    public static Result invokeExtensionRemoveMultiple(Path projectRoot, Path file) throws Exception {
        // add the qute extension
        Result result = execute(projectRoot, "extension", "remove", "amazon-lambda-http", "jackson", "-e", "-B", "--verbose");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);

        // list all extensions, make sure all are present
        result = invokeValidateExtensionList(projectRoot);
        Assertions.assertFalse(result.stdout.contains("quarkus-qute"),
                "quarkus-qute should not be in the list of extensions. Result:\n" + result);
        Assertions.assertFalse(result.stdout.contains("quarkus-amazon-lambda-http"),
                "quarkus-amazon-lambda-http should not be in the list of extensions. Result:\n" + result);
        Assertions.assertFalse(result.stdout.contains("quarkus-jackson"),
                "quarkus-jackson should not be in the list of extensions. Result:\n" + result);

        String content = CliDriver.readFileAsString(file);
        Assertions.assertFalse(content.contains("quarkus-qute"),
                "quarkus-qute should not be listed as a dependency. Result:\n" + content);
        Assertions.assertFalse(content.contains("quarkus-amazon-lambda-http"),
                "quarkus-amazon-lambda-http should not be listed as a dependency. Result:\n" + content);
        Assertions.assertFalse(content.contains("quarkus-jackson"),
                "quarkus-jackson should not be listed as a dependency. Result:\n" + content);

        return result;
    }

    public static Result invokeExtensionListInstallable(Path projectRoot) throws Exception {
        Result result = CliDriver.execute(projectRoot, "extension", "list", "-e", "-B", "--verbose", "-i");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);
        Assertions.assertTrue(result.stdout.contains("quarkus-hibernate-orm"),
                "quarkus-hibernate-orm should be listed as an installable extension. Found:\n" + result);
        Assertions.assertFalse(result.stdout.matches("quarkus-qute"),
                "quarkus-qute should not be listed as an installable extension. Found:\n" + result);
        return result;
    }

    public static Result invokeExtensionListInstallableSearch(Path projectRoot) throws Exception {
        Result result = CliDriver.execute(projectRoot, "extension", "list", "-e", "-B", "--verbose", "-i", "--search=vertx-*");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);

        Assertions.assertTrue(result.stdout.contains("quarkus-reactive-routes"),
                "quarkus-reactive-routes should be returned in search result. Found:\n" + result);
        Assertions.assertFalse(result.stdout.contains("quarkus-vertx-http"),
                "quarkus-vertx-http should not be returned in search result (already installed). Found:\n" + result);

        return result;
    }

    public static void invokeExtensionListFormatting(Path projectRoot) throws Exception {
        Result result = CliDriver.execute(projectRoot, "extension", "list", "-e", "-B", "--verbose", "-i", "--concise");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);
        Assertions.assertTrue(result.stdout.contains("quarkus-reactive-routes"),
                "quarkus-reactive-routes should be returned in result. Found:\n" + result);
        Assertions.assertTrue(result.stdout.contains("Reactive Routes"),
                "'Reactive Routes' descriptive name should be returned in results. Found:\n" + result);

        result = CliDriver.execute(projectRoot, "extension", "list", "-e", "-B", "--verbose", "-i", "--full");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);
        // TODO

        result = CliDriver.execute(projectRoot, "extension", "list", "-e", "-B", "--verbose", "-i", "--origins");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);
        // TODO

        // Two different output options can not be specified together
        result = CliDriver.execute(projectRoot, "extension", "list", "-e", "-B", "--verbose", "-i", "--origins", "--name");
        Assertions.assertEquals(CommandLine.ExitCode.USAGE, result.exitCode,
                "Expected OK return code. Result:\n" + result);
        // TODO
    }

    public static Result invokeExtensionAddRedundantQute(Path projectRoot) throws Exception {
        Result result = execute(projectRoot, "extension", "add", "-e", "-B", "--verbose", "qute");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);
        return result;
    }

    public static Result invokeExtensionRemoveNonexistent(Path projectRoot) throws Exception {
        Result result = execute(projectRoot, "extension", "remove", "-e", "-B", "--verbose", "nonexistent");
        System.out.println(result);
        return result;
    }

    public static Result invokeValidateDryRunBuild(Path projectRoot) throws Exception {
        Result result = execute(projectRoot, "build", "-e", "-B", "--dryrun",
                "-Dproperty=value1", "-Dproperty2=value2");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);
        Assertions.assertTrue(result.stdout.contains("Command line"),
                "--dry-run should echo command line");
        return result;
    }

    public static Result invokeValidateBuild(Path projectRoot) throws Exception {
        Result result = execute(projectRoot, "build", "-e", "-B", "--clean",
                "-Dproperty=value1", "-Dproperty2=value2");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);
        return result;
    }

    public static void validateApplicationProperties(Path projectRoot, List<String> configs) throws Exception {
        Path properties = projectRoot.resolve("src/main/resources/application.properties");
        Assertions.assertTrue(properties.toFile().exists(),
                "application.properties should exist: " + properties.toAbsolutePath().toString());
        String propertiesFile = CliDriver.readFileAsString(properties);
        configs.forEach(conf -> Assertions.assertTrue(propertiesFile.contains(conf),
                "Properties file should contain " + conf + ". Found:\n" + propertiesFile));
    }

    private static Optional<String> getMavenLocalRepoProperty() {
        return Optional.ofNullable(System.getProperty(LOCAL_REPO_PROPERTY));
    }

    private static Optional<String> getMavenSettingsProperty() {
        return Optional.ofNullable(System.getProperty(MAVEN_SETTINGS)).filter(value -> Files.exists(Path.of(value)));
    }

    private static void retryDelete(File file) {
        if (file.delete()) {
            return;
        }
        int i = 0;
        while (i++ < 10) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {

            }
            if (file.delete()) {
                break;
            }
        }
    }
}
