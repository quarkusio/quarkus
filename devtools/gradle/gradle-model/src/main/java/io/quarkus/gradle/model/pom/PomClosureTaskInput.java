package io.quarkus.gradle.model.pom;

import java.io.File;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;

import io.quarkus.maven.dependency.GAV;

/**
 * Serializable nested Gradle input for a prepared POM closure.
 * <p>
 * Coordinates and paths are exposed as scalar inputs, while resolved POM contents are exposed as a classpath input. The
 * object form reconstructed by {@link #getResult()} is internal to task execution. This separation gives Gradle stable
 * input normalization without requiring it to fingerprint Maven model objects.
 */
@SuppressWarnings("ClassCanBeRecord") // Gradle doesn't like records in this case
public final class PomClosureTaskInput implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Map<String, String> resolvedPomFilesByGav;
    private final List<String> missingPomGavs;
    private final List<File> resolvedPomFiles;

    private PomClosureTaskInput(Map<String, String> resolvedPomFilesByGav, List<String> missingPomGavs,
            List<File> resolvedPomFiles) {
        this.resolvedPomFilesByGav = Map.copyOf(resolvedPomFilesByGav);
        this.missingPomGavs = List.copyOf(missingPomGavs);
        this.resolvedPomFiles = List.copyOf(resolvedPomFiles);
    }

    /**
     * Creates a Gradle input snapshot from a closure.
     * <p>
     * Missing GAVs and the resolved-file list are canonicalized by GAV.
     *
     * @param result closure to normalize
     * @return serializable nested task input
     */
    public static PomClosureTaskInput from(PomClosureResult result) {
        Map<String, String> resolved = new TreeMap<>();
        result.resolvedPoms()
                .forEach((gav, file) -> resolved.put(gav.toString(), file.getAbsolutePath()));
        List<String> missing = result.missingPoms().stream()
                .map(GAV::toString)
                .sorted()
                .toList();
        List<File> files = resolved.values().stream()
                .map(File::new)
                .toList();
        return new PomClosureTaskInput(resolved, missing, files);
    }

    /** @return scalar mapping from POM GAV to absolute resolved path */
    @Input
    public Map<String, String> getResolvedPomFilesByGav() {
        return resolvedPomFilesByGav;
    }

    /** @return sorted scalar list of coordinates known to be missing */
    @Input
    public List<String> getMissingPomGavs() {
        return missingPomGavs;
    }

    /** @return resolved POM files tracked by Gradle using classpath normalization */
    @Classpath
    public List<File> getResolvedPomFiles() {
        return resolvedPomFiles;
    }

    /** @return the reconstructed closure consumed by the task action */
    @Internal
    public PomClosureResult getResult() {
        Map<GAV, File> resolved = new TreeMap<>((left, right) -> left.toString().compareTo(right.toString()));
        resolvedPomFilesByGav.forEach((gav, file) -> resolved.put(parseGav(gav), new File(file)));
        Set<GAV> missing = new TreeSet<>((left, right) -> left.toString().compareTo(right.toString()));
        missingPomGavs.stream().map(PomClosureTaskInput::parseGav).forEach(missing::add);
        return new PomClosureResult(resolved, missing);
    }

    private static GAV parseGav(String value) {
        String[] parts = value.split(":", -1);
        if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
            throw new IllegalArgumentException("POM closure GAV must have format groupId:artifactId:version: " + value);
        }
        return new GAV(parts[0], parts[1], parts[2]);
    }
}
