package io.quarkus.bootstrap.app;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * The result of an augmentation that builds an application
 */
public class AugmentResult {
    private final List<ArtifactResult> results;
    private final JarResult jar;
    private final Path nativeImagePath;

    public AugmentResult(List<ArtifactResult> results, JarResult jar, Path nativeImagePath) {
        this.results = results;
        this.jar = jar;
        this.nativeImagePath = nativeImagePath;
    }

    public List<ArtifactResult> getResults() {
        return results;
    }

    public JarResult getJar() {
        return jar;
    }

    public Path getNativeResult() {
        return nativeImagePath;
    }

    public List<ArtifactResult> resultsMatchingType(Predicate<String> typePredicate) {
        if (results == null) {
            return Collections.emptyList();
        }
        List<ArtifactResult> res = new ArrayList<>(1);
        for (ArtifactResult result : results) {
            if (typePredicate.test(result.getType())) {
                res.add(result);
            }
        }
        return res;
    }
}
