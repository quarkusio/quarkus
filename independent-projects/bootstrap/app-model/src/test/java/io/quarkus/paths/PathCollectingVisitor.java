package io.quarkus.paths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

class PathCollectingVisitor implements PathVisitor {

    final Map<String, String> visitedPaths = new HashMap<>();

    @Override
    public void visitPath(PathVisit visit) {
        visitedPaths.put(visit.getRelativePath(), toResourceName((getRelativeToRoot(visit))));
    }

    private static String toResourceName(Path path) {
        if (path.getNameCount() == 0) {
            return "";
        }
        var resourceName = new StringBuilder();
        resourceName.append(path.getName(0));
        for (int i = 1; i < path.getNameCount(); i++) {
            resourceName.append("/").append(path.getName(i));
        }
        return resourceName.toString();
    }

    private static Path getRelativeToRoot(PathVisit visit) {
        return Files.isDirectory(visit.getRoot())
                ? visit.getRoot().relativize(visit.getPath())
                : visit.getPath().getRoot().relativize(visit.getPath());
    }
}
