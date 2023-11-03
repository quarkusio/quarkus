package io.quarkus.paths;

import java.nio.file.Path;

public interface PathUtils {

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
            s.append('/').append(path.getName(i).toString());
        }
        return s.toString();
    }

}
