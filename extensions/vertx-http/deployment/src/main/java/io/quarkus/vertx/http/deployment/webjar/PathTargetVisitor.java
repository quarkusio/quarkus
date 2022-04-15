package io.quarkus.vertx.http.deployment.webjar;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Visitor which copies resources of the web jar to a given path.
 */
public class PathTargetVisitor implements WebJarResourcesTargetVisitor {
    private final Path deploymentPath;

    public PathTargetVisitor(Path deploymentPath) {
        this.deploymentPath = deploymentPath;
    }

    @Override
    public void visitDirectory(String path) throws IOException {
        Files.createDirectories(deploymentPath.resolve(path));
    }

    @Override
    public void visitFile(String path, InputStream stream) throws IOException {
        Path targetFilePath = deploymentPath.resolve(path);
        createFile(stream, targetFilePath);
    }

    @Override
    public boolean supportsOnlyCopyingNonArtifactFiles() {
        return true;
    }

    private static void createFile(InputStream source, Path targetFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(targetFile.toString())) {
            FileChannel channel = fos.getChannel();
            try (FileLock lock = channel.tryLock()) {
                if (lock != null) {
                    source.transferTo(fos);
                }
            }
        }
    }
}
