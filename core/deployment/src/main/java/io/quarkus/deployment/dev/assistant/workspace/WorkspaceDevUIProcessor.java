package io.quarkus.deployment.dev.assistant.workspace;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;

@BuildSteps(onlyIf = IsDevelopment.class)
class WorkspaceDevUIProcessor {

    @BuildStep
    void locateWorkspaceItems(BuildSystemTargetBuildItem buildSystemTarget,
            BuildProducer<UserWorkspaceBuildItem> workspaceProducer) {

        Path outputDir = buildSystemTarget.getOutputDirectory();
        Path projectRoot = outputDir.getParent();

        List<UserWorkspaceBuildItem.WorkspaceItem> workspaceItems = new ArrayList<>();

        try {
            Files.walkFileTree(projectRoot, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (Files.isHidden(dir)) {
                        return FileVisitResult.SKIP_SUBTREE; // Skip hidden directories and everything inside them
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (!Files.isHidden(file) && !file.startsWith(outputDir)) {
                        String name = projectRoot.relativize(file).toString();
                        workspaceItems.add(new UserWorkspaceBuildItem.WorkspaceItem(name, file));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        sortWorkspaceItems(workspaceItems);
        workspaceProducer.produce(new UserWorkspaceBuildItem(workspaceItems));
    }

    private void sortWorkspaceItems(List<UserWorkspaceBuildItem.WorkspaceItem> items) {
        items.sort(Comparator.comparing((UserWorkspaceBuildItem.WorkspaceItem item) -> isFileInRoot(item.name()))
                .thenComparing(item -> folderPriority(item.name()))
                .thenComparing(UserWorkspaceBuildItem.WorkspaceItem::name));
    }

    private static int folderPriority(String name) {
        if (name.startsWith("src/main/java"))
            return 1;
        if (name.startsWith("src/main/resources"))
            return 2;
        if (name.startsWith("src/main/"))
            return 3;
        if (name.startsWith("src/test/java"))
            return 4;
        if (name.startsWith("src/test/resources"))
            return 5;
        if (name.startsWith("src/test/"))
            return 6;
        if (name.startsWith("src/integrationTest/"))
            return 7;
        return 8;
    }

    private boolean isFileInRoot(String name) {
        return !name.contains("/");
    }

}
