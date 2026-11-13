package io.quarkus.gradle.model.pom;

import java.io.File;
import java.io.Serial;
import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;

import io.quarkus.maven.dependency.GAV;

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

    @Input
    public Map<String, String> getResolvedPomFilesByGav() {
        return resolvedPomFilesByGav;
    }

    @Input
    public List<String> getMissingPomGavs() {
        return missingPomGavs;
    }

    @Classpath
    public List<File> getResolvedPomFiles() {
        return resolvedPomFiles;
    }

    @Internal
    public PomClosureResult getResult() {
        Map<GAV, File> resolved = new TreeMap<>(Comparator.comparing(GAV::toString));
        resolvedPomFilesByGav.forEach((gav, file) -> resolved.put(PomClosureResultCodec.parseGav(gav), new File(file)));
        Set<GAV> missing = new TreeSet<>(Comparator.comparing(GAV::toString));
        missingPomGavs.stream().map(PomClosureResultCodec::parseGav).forEach(missing::add);
        return new PomClosureResult(resolved, missing);
    }
}
