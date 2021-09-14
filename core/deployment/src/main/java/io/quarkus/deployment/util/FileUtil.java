package io.quarkus.deployment.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtil {

    public static void deleteDirectory(final Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return;
        }
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                try {
                    Files.delete(file);
                } catch (IOException e) {
                    // ignored
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                try {
                    Files.delete(dir);
                } catch (IOException e) {
                    // ignored
                }
                return FileVisitResult.CONTINUE;
            }

        });
    }

    public static byte[] readFileContents(InputStream inputStream) throws IOException {
        return inputStream.readAllBytes();
    }

    /**
     * Translates a file path from the Windows Style to a syntax accepted by Docker,
     * so that volumes be safely mounted in both Docker for Windows and the legacy
     * Docker Toolbox.
     * <p>
     * <code>docker run -v //c/foo/bar:/somewhere (...)</code>
     * <p>
     * You should only use this method on Windows-style paths, and not Unix-style
     * paths.
     * 
     * @see https://github.com/quarkusio/quarkus/issues/5360
     * @param windowsStylePath A path formatted in Windows-style, e.g. "C:\foo\bar".
     * @return A translated path accepted by Docker, e.g. "//c/foo/bar".
     */
    public static String translateToVolumePath(String windowsStylePath) {
        String translated = windowsStylePath.replace('\\', '/');
        Pattern p = Pattern.compile("^(\\w)(?:$|:(/)?(.*))");
        Matcher m = p.matcher(translated);
        if (m.matches()) {
            String slash = Optional.ofNullable(m.group(2)).orElse("/");
            String path = Optional.ofNullable(m.group(3)).orElse("");
            return "//" + m.group(1).toLowerCase() + slash + path;
        }
        return translated;
    }
}
