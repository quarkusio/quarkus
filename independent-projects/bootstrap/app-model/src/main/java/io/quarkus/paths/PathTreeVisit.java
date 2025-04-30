package io.quarkus.paths;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

class PathTreeVisit implements PathVisit {

    static void walk(Path root, Path rootDir, Path walkDir, PathFilter pathFilter, Map<String, String> multiReleaseMapping,
            PathVisitor visitor) {
        final PathTreeVisit visit = new PathTreeVisit(root, rootDir, pathFilter, multiReleaseMapping);
        try (Stream<Path> files = Files.walk(walkDir)) {
            final Iterator<Path> i = files.iterator();
            while (i.hasNext()) {
                if (!visit.setCurrent(i.next())) {
                    continue;
                }
                visitor.visitPath(visit);
                if (visit.isStopWalking()) {
                    break;
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to walk directory " + root, e);
        }
        visit.visitMultiReleasePaths(visitor);
    }

    static <T> T process(Path root, Path rootDir, Path path, PathFilter pathFilter, Function<PathVisit, T> func) {
        final PathTreeVisit visit = new PathTreeVisit(root, rootDir, pathFilter, Map.of());
        if (visit.setCurrent(path)) {
            return func.apply(visit);
        }
        return func.apply(null);
    }

    static void consume(Path root, Path rootDir, Path path, PathFilter pathFilter, Consumer<PathVisit> func) {
        final PathTreeVisit visit = new PathTreeVisit(root, rootDir, pathFilter, Map.of());
        if (visit.setCurrent(path)) {
            func.accept(visit);
        } else {
            func.accept(null);
        }
    }

    private final Path root;
    private final Path baseDir;
    private final PathFilter pathFilter;
    private final Map<String, String> multiReleaseMapping;

    private Path current;
    private String relativePath;
    private boolean stopWalking;

    private PathTreeVisit(Path root, Path rootDir, PathFilter pathFilter, Map<String, String> multiReleaseMapping) {
        this.root = root;
        this.baseDir = rootDir;
        this.pathFilter = pathFilter;
        this.multiReleaseMapping = multiReleaseMapping == null || multiReleaseMapping.isEmpty() ? Map.of()
                : new HashMap<>(multiReleaseMapping);
    }

    @Override
    public Path getRoot() {
        return root;
    }

    @Override
    public Path getPath() {
        return current;
    }

    @Override
    public void stopWalking() {
        stopWalking = true;
    }

    boolean isStopWalking() {
        return stopWalking;
    }

    @Override
    public String getRelativePath(String separator) {
        if (relativePath == null) {
            return PathTreeUtils.asString(baseDir.relativize(current), separator);
        }
        if (!current.getFileSystem().getSeparator().equals(separator)) {
            return relativePath.replace(current.getFileSystem().getSeparator(), separator);
        }
        return relativePath;
    }

    private boolean setCurrent(Path path) {
        current = path;
        relativePath = null;
        if (pathFilter != null) {
            relativePath = baseDir.relativize(path).toString();
            if (!PathFilter.isVisible(pathFilter, relativePath)) {
                return false;
            }
        }
        if (!multiReleaseMapping.isEmpty()) {
            if (relativePath == null) {
                relativePath = baseDir.relativize(path).toString();
            }
            final String mrPath = multiReleaseMapping.remove(relativePath);
            if (mrPath != null) {
                current = baseDir.resolve(mrPath);
            }
        }
        return true;
    }

    private void visitMultiReleasePaths(PathVisitor visitor) {
        for (Map.Entry<String, String> mrEntry : multiReleaseMapping.entrySet()) {
            relativePath = mrEntry.getKey();
            if (pathFilter != null && !PathFilter.isVisible(pathFilter, relativePath)) {
                continue;
            }
            current = baseDir.resolve(mrEntry.getValue());
            visitor.visitPath(this);
        }
    }
}
