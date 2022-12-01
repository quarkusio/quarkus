package io.quarkus.cli.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class GradleInitScript {

    public static final String ALL_PROJECTS = "allprojects {";
    public static final String APPLY_PLUGIN_JAVA = "apply plugin: 'java'";
    public static final String DEPENDENCIES = "dependencies {";
    public static final String DEPENDENCY = "implementation '%s'";
    public static final String CLOSE = "}";
    public static final String TAB = "    ";
    public static final String NEWLINE = "\n";

    /**
     * Create an init script that adds the specidied extensions and populate the arguments
     * that should be passed to the gradle command, so that it loads the generated init script.
     * The command will append to the specified argument list: `--init-script=</path/to/temporary/initscript>`
     *
     * @param forcedExtensions The forcced extension to add to the init script
     * @param args The argument list
     */
    public static void populateForExtensions(Collection<String> forcedExtensions, Collection<String> args) {
        List<String> gavs = forcedExtensions.stream()
                .map(String::trim)
                .map(e -> e + ":${quarkusPlatformVersion}")
                .collect(Collectors.toList());
        Path initScriptPath = GradleInitScript.createInitScript(gavs);
        args.add("--init-script=" + initScriptPath.toAbsolutePath().toString());
    }

    public static Path createInitScript(List<String> gavs) {
        try {
            Path path = Files.createTempFile("quarkus-gradle-init", "");
            createInitScript(path, gavs);
            return path;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void createInitScript(Path path, List<String> gavs) {
        StringBuilder sb = new StringBuilder();
        sb.append(ALL_PROJECTS).append(NEWLINE);
        sb.append(TAB).append(APPLY_PLUGIN_JAVA).append(NEWLINE);
        sb.append(TAB).append(DEPENDENCIES).append(NEWLINE);

        for (String gav : gavs) {
            sb.append(TAB).append(TAB).append(String.format(DEPENDENCY, gav)).append(NEWLINE);
        }

        sb.append(TAB).append(CLOSE).append(NEWLINE);
        sb.append(CLOSE).append(NEWLINE);
        try {
            Files.writeString(path, sb.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
