package io.quarkus.cli.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GradleInitScript {

    public static final String ALL_PROJECTS = "allprojects {";
    public static final String APPLY_PLUGIN_JAVA = "apply plugin: 'java'";
    public static final String DEPENDENCIES = "dependencies {";
    public static final String DEPENDENCY = "implementation '%s'";
    public static final String CLOSE = "}";
    public static final String TAB = "    ";
    public static final String NEWLINE = "\n";

    public static final String INIT_SCRIPT = "--init-script=";

    /**
     * Get the path of the init script if found in the list of arguments.
     *
     * @return The optional path.
     */
    public static Optional<String> getInitScript(Collection<String> args) {
        return args.stream().filter(s -> s.contains(INIT_SCRIPT)).map(s -> s.substring(INIT_SCRIPT.length()))
                .findFirst();
    }

    /**
     * Create an init script that adds the specidied extensions and populate the arguments that should be passed to the
     * gradle command, so that it loads the generated init script. The command will append to the specified argument
     * list: `--init-script=</path/to/temporary/initscript>`
     *
     * @param forcedExtensions
     *        The forcced extension to add to the init script
     * @param args
     *        The argument list
     */
    public static void populateForExtensions(Collection<String> forcedExtensions, Collection<String> args) {
        Optional<String> existingInitScript = getInitScript(args);
        List<String> existingGavs = existingInitScript.map(s -> Path.of(s)).map(p -> readInitScriptDependencies(p))
                .orElse(new ArrayList<String>());

        Set<String> gavs = Stream
                .concat(existingGavs.stream(),
                        forcedExtensions.stream().map(String::trim).map(e -> e + ":${quarkusPlatformVersion}"))
                .collect(Collectors.toSet());

        existingInitScript.map(Path::of).ifPresentOrElse(s -> createInitScript(s, gavs), () -> {
            Path initScriptPath = GradleInitScript.createInitScript(gavs);
            args.add(INIT_SCRIPT + initScriptPath.toAbsolutePath().toString());
        });
    }

    /**
     * Return the GAV of the dependencies found in the specified init script.
     *
     * @return a list with gavs found.
     */
    public static List<String> readInitScriptDependencies(Path path) {
        try {
            return Arrays.stream(Files.readString(path).split(NEWLINE)).filter(l -> l.contains("implementation"))
                    .map(s -> s.replaceAll("^[  ]*implementation[  ]*", "").replaceAll("'", ""))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Path createInitScript(Set<String> gavs) {
        try {
            Path path = Files.createTempFile("quarkus-gradle-init", "");
            createInitScript(path, gavs);
            return path;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void createInitScript(Path path, Set<String> gavs) {
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
