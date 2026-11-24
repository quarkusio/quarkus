package io.quarkus.gradle.tooling;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.MappableCollectionFactory;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.maven.dependency.ResolvedDependency;

/**
 * A rendering of an application model that names files by what they are instead of where they are, for use as a
 * build cache key.
 * <p>
 * The location of every file the model refers to is replaced by the identity the model itself gives it: the
 * coordinates of the artifact a path was resolved for, or the ID of the workspace module a directory belongs to. No
 * directory has to be known in advance, so an artifact renders the same way wherever it was resolved from: a Gradle
 * user home, a Maven repository, another project of the build or an included build.
 * <p>
 * The digest says which artifacts make up the application, not what they contain. A task hashing it must also
 * declare the files as inputs with a content-based normalization, such as {@code @Classpath}.
 * <p>
 * A path the model does not attribute to an artifact or a module is kept as is, which can only cost a cache miss.
 */
public final class ApplicationModelDigest {

    /**
     * The first line of every digest. Change it whenever the rendering changes, so that digests written by an
     * earlier version never match the ones written by a later one.
     */
    static final String HEADER = "# Quarkus application model digest, version 1";

    private ApplicationModelDigest() {
    }

    /**
     * Writes the digest of a model to a file.
     *
     * @param model the application model
     * @param file the file to write
     * @throws IOException if the file cannot be written
     */
    public static void write(ApplicationModel model, Path file) throws IOException {
        Files.writeString(file, render(model), StandardCharsets.UTF_8);
    }

    /**
     * Renders the digest of a model: one line per value of the model, in the order the model is serialized, with map
     * keys sorted, so that two digests can be compared with a text diff.
     *
     * @param model the application model
     * @return the digest
     */
    public static String render(ApplicationModel model) {
        final Map<String, String> names = new LinkedHashMap<>();
        collect(model.getAppArtifact(), names);
        for (ResolvedDependency dep : model.getDependencies()) {
            collect(dep, names);
        }
        final List<Map.Entry<String, String>> locations = new ArrayList<>(names.entrySet());
        // the longest first, so that a classes directory is named after its artifact rather than after the module
        // directory it is in
        locations.sort(Comparator.comparingInt((Map.Entry<String, String> e) -> e.getKey().length()).reversed()
                .thenComparing(Map.Entry::getKey));

        final StringBuilder out = new StringBuilder();
        out.append(HEADER).append('\n');
        render("", model.asMap(MappableCollectionFactory.defaultInstance()), locations, out);
        return out.toString();
    }

    private static void collect(ResolvedDependency dep, Map<String, String> names) {
        int i = 0;
        for (Path path : dep.getResolvedPaths()) {
            names.putIfAbsent(path.toString(), "${artifact:" + dep.toGACTVString() + "#" + i++ + "}");
        }
        final WorkspaceModule module = dep.getWorkspaceModule();
        if (module != null) {
            add(module.getBuildDir(), "${module-build:" + module.getId() + "}", names);
            add(module.getModuleDir(), "${module:" + module.getId() + "}", names);
        }
    }

    private static void add(File dir, String name, Map<String, String> names) {
        if (dir != null) {
            names.putIfAbsent(dir.toString(), name);
        }
    }

    private static void render(String pointer, Object value, List<Map.Entry<String, String>> locations, StringBuilder out) {
        if (value instanceof Map<?, ?> map) {
            // sorted, since the model holds some of its maps as HashMaps
            final Map<String, Object> sorted = new TreeMap<>();
            map.forEach((k, v) -> sorted.put(String.valueOf(k), v));
            if (sorted.isEmpty()) {
                out.append(pointer).append("={}\n");
            }
            sorted.forEach((k, v) -> render(pointer + "/" + k, v, locations, out));
        } else if (value instanceof Collection<?> collection) {
            if (collection.isEmpty()) {
                out.append(pointer).append("=[]\n");
            }
            int i = 0;
            for (Object element : collection) {
                render(pointer + "[" + i++ + "]", element, locations, out);
            }
        } else {
            out.append(pointer).append('=');
            final String rendered = value instanceof String s ? name(s, locations) : String.valueOf(value);
            out.append(rendered.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r"));
            out.append('\n');
        }
    }

    static String name(String value, List<Map.Entry<String, String>> locations) {
        for (Map.Entry<String, String> location : locations) {
            final String path = location.getKey();
            if (value.startsWith(path)) {
                if (value.length() == path.length()) {
                    return location.getValue();
                }
                final char next = value.charAt(path.length());
                if (next == '/' || next == '\\') {
                    return location.getValue() + value.substring(path.length()).replace('\\', '/');
                }
            }
        }
        return value;
    }
}
