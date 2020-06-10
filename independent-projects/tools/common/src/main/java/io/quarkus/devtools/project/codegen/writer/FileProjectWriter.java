/**
 *
 */
package io.quarkus.devtools.project.codegen.writer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * ProjectWriter implementation to create direct files in the file system.
 */
public class FileProjectWriter implements ProjectWriter {
    private final File root;

    public FileProjectWriter(final File file) {
        root = file;
    }

    @Override
    public void init() throws IOException {
        if (!root.exists()) {
            boolean mkdirStatus = root.mkdirs();
            if (!mkdirStatus) {
                throw new IOException("Failed to create root directory");
            }
            return;
        }
        if (!root.isDirectory()) {
            throw new IOException("Project root needs to be a directory");
        }
        final String[] files = root.list();
        if (files != null && files.length > 0) {
            throw new IOException("You can't create a project when the folder is not empty.");
        }
    }

    @Override
    public String mkdirs(String path) {
        File dirToCreate = new File(root, path);
        if (!dirToCreate.exists()) {
            dirToCreate.mkdirs();
        }
        if (path.isEmpty()) {
            return "";
        }
        return dirToCreate.getPath().substring(root.getPath().length() + 1);
    }

    @Override
    public void write(String path, String content) throws IOException {
        final Path outputPath = root.toPath().resolve(path);
        Files.write(outputPath, content.getBytes("UTF-8"));
    }

    @Override
    public byte[] getContent(String path) throws IOException {
        return Files.readAllBytes(root.toPath().resolve(path));
    }

    @Override
    public boolean exists(String path) {
        return new File(root, path).exists();
    }

    @Override
    public void close() throws IOException {
        //do nothing
    }

    @Override
    public File getProjectFolder() {
        return root;
    }

    @Override
    public boolean hasFile() {
        return true;
    }

}
