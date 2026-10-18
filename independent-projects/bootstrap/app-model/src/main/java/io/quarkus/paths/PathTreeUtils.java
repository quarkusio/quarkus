package io.quarkus.paths;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public interface PathTreeUtils {

    /**
     * Returns a path as a string using the specified separator.
     *
     * @param path path to convert to a string
     * @param separator path element separator
     * @return string representation of a path
     */
    static String asString(final Path path, String separator) {
        if (path.getFileSystem().getSeparator().equals(separator)) {
            return path.toString();
        }
        final int nameCount = path.getNameCount();
        if (nameCount == 0) {
            return "";
        }
        if (nameCount == 1) {
            return path.getName(0).toString();
        }
        final StringBuilder s = new StringBuilder();
        s.append(path.getName(0));
        for (int i = 1; i < nameCount; ++i) {
            s.append(separator).append(path.getName(i));
        }
        return s.toString();
    }

    /**
     * Checks whether a path tree contains a given relative path respecting case sensitivity even on Windows.
     *
     * <p>
     * Path API on Windows may resolve {@code templates} to {@code Templates}. This method
     * helps verify whether a given relative path actually exists.
     *
     * @param pathTree path tree
     * @param relativePath relative path to check
     * @return true if a path tree contains a given relative path
     */
    static boolean containsCaseSensitivePath(PathTree pathTree, String relativePath) {
        if (!pathTree.contains(relativePath)) {
            return false;
        }
        // if it's not Windows, we don't need to check further
        if (File.separatorChar != '\\') {
            return true;
        }
        // this should not be necessary, since relatvePath is meant to be a resource path, not an FS path but just in case
        relativePath = relativePath.replace(File.separatorChar, '/');
        final String[] pathElements = relativePath.split("/");
        try (var openTree = pathTree.open()) {
            for (var root : openTree.getRoots()) {
                if (containsCaseSensitivePath(root, pathElements)) {
                    return true;
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return false;
    }

    private static boolean containsCaseSensitivePath(Path root, String[] pathElements) {
        var parent = root;
        for (String pathElement : pathElements) {
            if (!Files.isDirectory(parent)) {
                return false;
            }
            try (Stream<Path> stream = Files.list(parent)) {
                var i = stream.iterator();
                Path match = null;
                while (i.hasNext()) {
                    final Path next = i.next();
                    if (pathElement.equals(next.getFileName().toString())) {
                        match = next;
                        break;
                    }
                }
                if (match == null) {
                    return false;
                }
                parent = match;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (Exception e) {
                throw e;
            }
        }
        return true;
    }
}
