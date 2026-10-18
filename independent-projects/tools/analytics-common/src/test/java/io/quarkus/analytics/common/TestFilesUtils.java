package io.quarkus.analytics.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class TestFilesUtils {
    public static void restore(Path backupPath, Path targetPath) throws IOException {
        if (Files.exists(backupPath)) {
            // restore existing
            Files.move(backupPath,
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING);
        } else {
            // delete file generated in test
            Files.delete(targetPath);
        }
    }

    public static void backupExisting(Path backupPath, Path targetPath) throws IOException {
        if (Files.exists(targetPath)) {
            Files.move(targetPath,
                    backupPath,
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
