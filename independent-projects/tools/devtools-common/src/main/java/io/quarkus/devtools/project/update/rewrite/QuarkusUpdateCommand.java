package io.quarkus.devtools.project.update.rewrite;

import static io.quarkus.devtools.project.update.rewrite.QuarkusUpdateRecipe.RECIPE_IO_QUARKUS_OPENREWRITE_QUARKUS;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.qute.Qute;

public class QuarkusUpdateCommand {

    public static final String MAVEN_REWRITE_PLUGIN_GROUP = "org.openrewrite.maven";
    public static final String MAVEN_REWRITE_PLUGIN_ARTIFACT = "rewrite-maven-plugin";
    public static Set<String> ADDITIONAL_SOURCE_FILES_SET = Set.of("**/META-INF/services/**",
            "**/*.txt",
            "**/*.adoc",
            "**/*.md",
            "**/src/main/codestarts/**/*.java",
            "**/src/test/resources/__snapshots__/**/*.java",
            "**/*.kt");

    public static String ADDITIONAL_SOURCE_FILES = String.join(",", ADDITIONAL_SOURCE_FILES_SET);

    public static String goal(boolean dryRun) {
        return dryRun ? "dryRun" : "run";
    }

    public static void handle(MessageWriter log, BuildTool buildTool, Path baseDir,
            String rewritePluginVersion, String recipesGAV, Path recipe, boolean dryRun) throws QuarkusUpdateException {
        switch (buildTool) {
            case MAVEN:
                runMavenUpdate(log, baseDir, rewritePluginVersion, recipesGAV, recipe, dryRun);
                break;
            case GRADLE:
                runGradleUpdate(log, baseDir, rewritePluginVersion, recipesGAV, recipe, dryRun);
                break;
            default:
                throw new QuarkusUpdateException(buildTool.getKey() + " is not supported yet");
        }
    }

    private static void runGradleUpdate(MessageWriter log, Path baseDir, String rewritePluginVersion, String recipesGAV,
            Path recipe, boolean dryRun) throws QuarkusUpdateException {
        Path tempInit = null;
        try {
            tempInit = Files.createTempFile("openrewrite-init", "gradle");
            try (InputStream inputStream = QuarkusUpdateCommand.class.getResourceAsStream("/openrewrite-init.gradle")) {
                String template = new BufferedReader(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.joining("\n"));
                Files.writeString(tempInit,
                        Qute.fmt(template, Map.of(
                                "rewriteFile", recipe.toAbsolutePath().toString(),
                                "pluginVersion", rewritePluginVersion,
                                "recipesGAV", recipesGAV,
                                "activeRecipe", RECIPE_IO_QUARKUS_OPENREWRITE_QUARKUS,
                                "plainTextMask", ADDITIONAL_SOURCE_FILES_SET.stream()
                                        .map(s -> "\"" + s + "\"")
                                        .collect(Collectors.joining(", ")))));
            }
            final String gradleBinary = findGradleBinary(baseDir);
            String[] command = new String[] { gradleBinary.toString(), "--console", "plain", "--stacktrace",
                    "--init-script",
                    tempInit.toAbsolutePath().toString(), dryRun ? "rewriteDryRun" : "rewriteRun" };
            executeCommand(baseDir, command, log);
        } catch (Exception e) {
            throw new QuarkusUpdateException("Error while running Gradle rewrite command", e);
        } finally {
            if (tempInit != null) {
                try {
                    Files.deleteIfExists(tempInit);
                } catch (Exception e) {
                    // ignore
                }
            }

        }
    }

    private static void runMavenUpdate(MessageWriter log, Path baseDir, String rewritePluginVersion, String recipesGAV,
            Path recipe,
            boolean dryRun) throws QuarkusUpdateException {
        final String mvnBinary = findMvnBinary(baseDir);
        executeCommand(baseDir, getMavenUpdateCommand(mvnBinary, rewritePluginVersion, recipesGAV, recipe, dryRun), log);

        // format the sources
        executeCommand(baseDir, getMavenProcessSourcesCommand(mvnBinary), log);
    }

    private static String[] getMavenProcessSourcesCommand(String mvnBinary) {
        return new String[] { mvnBinary, "process-sources" };
    }

    private static String[] getMavenUpdateCommand(String mvnBinary, String rewritePluginVersion, String recipesGAV, Path recipe,
            boolean dryRun) {
        return new String[] { mvnBinary,
                "-e",
                String.format("%s:%s:%s:%s", MAVEN_REWRITE_PLUGIN_GROUP, MAVEN_REWRITE_PLUGIN_ARTIFACT, rewritePluginVersion,
                        dryRun ? "dryRun" : "run"),
                String.format("-DplainTextMasks=%s", ADDITIONAL_SOURCE_FILES),
                String.format("-Drewrite.configLocation=%s", recipe.toAbsolutePath()),
                String.format("-Drewrite.recipeArtifactCoordinates=%s", recipesGAV),
                String.format("-DactiveRecipes=%s", RECIPE_IO_QUARKUS_OPENREWRITE_QUARKUS),
                "-Drewrite.pomCacheEnabled=false" };
    }

    private static void executeCommand(Path baseDir, String[] command, MessageWriter log) throws QuarkusUpdateException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        log.info("");
        log.info("");
        log.info("");
        log.info(" ------------------------------------------------------------------------");
        log.info("Executing:\n" + String.join(" ", command));
        log.info("");
        processBuilder.command(command);

        try {
            Process process = processBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectError(ProcessBuilder.Redirect.PIPE).start();

            BufferedReader inputReader = new BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new java.io.InputStreamReader(process.getErrorStream()));

            String line;
            while ((line = inputReader.readLine()) != null) {
                log.info(line);
            }
            while ((line = errorReader.readLine()) != null) {
                log.error(line);
            }

            log.info("");
            log.info("");
            log.info("");

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new QuarkusUpdateException("The command to update the project exited with an error: " + exitCode);
            }
        } catch (Exception e) {
            throw new QuarkusUpdateException("Error while executing command to udpate the project", e);
        }
    }

    static String findMvnBinary(Path baseDir) throws QuarkusUpdateException {
        Path mavenCmd = findWrapperOrBinary(baseDir, "mvnw", "mvn");
        if (mavenCmd == null) {
            throw new QuarkusUpdateException("Cannot locate mvnw or mvn"
                    + ". Make sure mvnw is in the project directory or mvn is in your PATH.");
        }
        return mavenCmd.toString();
    }

    static String findGradleBinary(Path baseDir) throws QuarkusUpdateException {
        Path gradleCmd = findWrapperOrBinary(baseDir, "gradlew", "gradle");
        if (gradleCmd == null) {
            throw new QuarkusUpdateException("Cannot gradlew mvnw or gradle"
                    + ". Make sure gradlew is in the current directory or gradle in your PATH.");
        }
        return gradleCmd.toString();
    }

    private static Path findWrapperOrBinary(Path baseDir, String wrapper, String cmd) {
        Path found = searchPath(wrapper, baseDir.toString());
        if (found == null) {
            found = searchPath(cmd);
        }
        return found;
    }

    /**
     * Searches the locations defined by PATH for the given executable
     *
     * @param cmd The name of the executable to look for
     * @return A Path to the executable, if found, null otherwise
     */
    public static Path searchPath(String cmd) {
        String envPath = System.getenv("PATH");
        envPath = envPath != null ? envPath : "";
        return searchPath(cmd, envPath);
    }

    /**
     * Searches the locations defined by `paths` for the given executable
     *
     * @param cmd The name of the executable to look for
     * @param paths A string containing the paths to search
     * @return A Path to the executable, if found, null otherwise
     */
    public static Path searchPath(String cmd, String paths) {
        return Arrays.stream(paths.split(File.pathSeparator))
                .map(dir -> Paths.get(dir).resolve(cmd))
                .flatMap(QuarkusUpdateCommand::executables)
                .filter(QuarkusUpdateCommand::isExecutable)
                .findFirst()
                .orElse(null);
    }

    private static Stream<Path> executables(Path base) {
        if (isWindows()) {
            return Stream.of(Paths.get(base.toString() + ".exe"),
                    Paths.get(base.toString() + ".bat"),
                    Paths.get(base.toString() + ".cmd"),
                    Paths.get(base.toString() + ".ps1"));
        } else {
            return Stream.of(base);
        }
    }

    private static boolean isExecutable(Path file) {
        if (Files.isRegularFile(file)) {
            if (isWindows()) {
                String nm = file.getFileName().toString().toLowerCase();
                return nm.endsWith(".exe") || nm.endsWith(".bat") || nm.endsWith(".cmd") || nm.endsWith(".ps1");
            } else {
                return Files.isExecutable(file);
            }
        }
        return false;
    }

    private static String OS = System.getProperty("os.name").toLowerCase();

    public static boolean isWindows() {
        return OS.contains("win");
    }

    static boolean hasGradle(Path dir) {
        return Files.exists(dir.resolve("build.gradle"));
    }

    private static boolean hasMaven(Path dir) {
        return Files.exists(dir.resolve("pom.xml"));
    }

}
