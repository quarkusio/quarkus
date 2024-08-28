package io.quarkus.tls.cli;

import static io.quarkus.tls.cli.letsencrypt.LetsEncryptConstants.DOT_ENV_FILE;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class DotEnvHelper {

    private DotEnvHelper() {
        // Avoid direct instantiation
    }

    public static List<String> readDotEnvFile() throws IOException {
        if (!DOT_ENV_FILE.exists()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Files.readAllLines(DOT_ENV_FILE.toPath()));
    }

    public static void addOrReplaceProperty(List<String> content, String key, String value) {
        var line = hasLine(content, key);
        if (line != -1) {
            content.set(line, key + "=" + value);
        } else {
            content.add(key + "=" + value);
        }
    }

    private static int hasLine(List<String> content, String key) {
        for (int i = 0; i < content.size(); i++) {
            if (content.get(i).startsWith(key + "=") || content.get(i).startsWith(key + " =")) {
                return i;
            }
        }
        return -1;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void deleteQuietly(File file) {
        if (file.isFile()) {
            file.delete();
        }
    }
}
