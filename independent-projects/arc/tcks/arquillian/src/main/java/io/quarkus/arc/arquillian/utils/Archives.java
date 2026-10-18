package io.quarkus.arc.arquillian.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Pattern;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.asset.Asset;

public class Archives {
    public static void explode(Archive<?> archive, String prefix, Path targetPath) throws IOException {
        String prefixPattern = "^" + Pattern.quote(prefix);
        Map<ArchivePath, Node> files = archive.getContent(Filters.include(prefixPattern + ".*"));
        for (Map.Entry<ArchivePath, Node> entry : files.entrySet()) {
            Asset asset = entry.getValue().getAsset();
            if (asset == null) {
                continue;
            }

            String path = entry.getKey().get().replaceFirst(prefixPattern, "");
            copy(asset, targetPath.resolve(path));
        }
    }

    public static void copy(Asset asset, Path targetPath) throws IOException {
        Files.createDirectories(targetPath.getParent()); // make sure the directory exists
        try (InputStream in = asset.openStream()) {
            Files.copy(in, targetPath);
        }
    }
}
