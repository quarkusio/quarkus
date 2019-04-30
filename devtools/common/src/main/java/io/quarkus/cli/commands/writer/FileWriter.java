/**
 * 
 */
package io.quarkus.cli.commands.writer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author guillaumedufour
 *
 */
public class FileWriter implements Writer {
    private final File root;

    public FileWriter(final File file) {
        root = file;
    }

    @Override
    public boolean init() {
        if (root.exists() && !root.isDirectory()) {
            System.out.println("Project root needs to either not exist or be a directory");
            return false;
        } else if (!root.exists()) {
            boolean mkdirStatus = root.mkdirs();
            if (!mkdirStatus) {
                System.out.println("Failed to create root directory");
                return false;
            }
        }

        System.out.println("Creating a new project in " + root.getAbsolutePath());

        return true;
    }

    @Override
    public String mkdirs(String path) {
        File dirToCreate = new File(root, path);
        if (!dirToCreate.exists()) {
            dirToCreate.mkdirs();
        }
        if (path.isEmpty()) {
            if (root.getPath().isEmpty()) {
                return "";
            }
            return "/";
        }
        return dirToCreate.getPath().substring(root.getPath().length() + 1) + "/";
    }

    @Override
    public void write(String path, String content) throws IOException {
        final Path outputPath = Paths.get(root + "/" + path);
        Files.write(outputPath, content.getBytes());
    }

    @Override
    public byte[] getContent(String path) throws IOException {
        return Files.readAllBytes(Paths.get(root + "/" + path));
    }

    @Override
    public boolean exists(String path) {
        return new File(root, path).exists();
    }

}
