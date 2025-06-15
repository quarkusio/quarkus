package io.quarkus.vertx.http.runtime.webjar;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.jboss.logging.Logger;

import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class WebJarRecorder {

    private static final Logger LOG = Logger.getLogger(WebJarRecorder.class);

    public void shutdownTask(ShutdownContext shutdownContext, String deploymentBasePath) {

        shutdownContext.addShutdownTask(new DeleteDirectoryRunnable(deploymentBasePath));
    }

    private static final class DeleteDirectoryRunnable implements Runnable {

        private final Path directory;

        private DeleteDirectoryRunnable(String directory) {
            this.directory = Paths.get(directory);
        }

        @Override
        public void run() {
            try {
                Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                LOG.error("Error cleaning up webjar temporary directory: " + directory, e);
            }
        }
    }
}
