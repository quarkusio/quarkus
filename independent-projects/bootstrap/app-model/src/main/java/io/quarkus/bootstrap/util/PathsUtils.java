package io.quarkus.bootstrap.util;

import io.quarkus.bootstrap.model.PathsCollection;
import java.io.File;
import java.util.Collection;

public class PathsUtils {

    public static PathsCollection toPathsCollection(Collection<File> c) {
        final PathsCollection.Builder builder = PathsCollection.builder();
        for (File f : c) {
            builder.add(f.toPath());
        }
        return builder.build();
    }
}
