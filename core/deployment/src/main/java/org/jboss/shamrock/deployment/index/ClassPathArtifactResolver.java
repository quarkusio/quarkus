package org.jboss.shamrock.deployment.index;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class path based runner that can resolve artifacts from the current class path.
 * <p>
 * This assumes that all artifacts to be resolved have a META-INF/MANIFEST.MF file,
 * and are layed out in the maven repository structure on the file system.
 */
public class ClassPathArtifactResolver implements ArtifactResolver {

    private static final String META_INF_MANIFEST_MF = "META-INF/MANIFEST.MF";
    private final List<StoredUrl> pathList = new ArrayList<>();

    public ClassPathArtifactResolver(ClassLoader classLoader) {
        ClassLoader cl = classLoader;
        try {
            Enumeration<URL> res = cl.getResources(META_INF_MANIFEST_MF);
            while (res.hasMoreElements()) {
                URL jarUrl = res.nextElement();
                String path = jarUrl.getPath();
                if (path.startsWith("file:")) {
                    path = path.substring(5, path.length() - META_INF_MANIFEST_MF.length() - 2);
                    String[] parts = path.split("/"); //TODO: windows?
                    String fileName = parts[parts.length - 1];
                    List<String> fileParts = new ArrayList<>();
                    for (int i = parts.length - 2; i >= 0; --i) {
                        fileParts.add(parts[i]);
                    }
                    pathList.add(new StoredUrl(Paths.get(path), fileName, fileParts));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ResolvedArtifact getArtifact(String groupId, String artifactId, String classifier) {
        Pattern filePatten;
        if (classifier == null || classifier.isEmpty()) {
            filePatten = Pattern.compile(artifactId + "-(\\d.*)\\.jar");
        } else {
            filePatten = Pattern.compile(artifactId + "-" + classifier + "-(\\d.*)\\.jar");
        }
        for (StoredUrl url : pathList) {
            Matcher matcher = filePatten.matcher(url.fileName);
            if (matcher.matches()) {
                String[] groupParts = groupId.split("\\.");
                if (url.reverseParts.size() < groupParts.length + 2) {
                    continue;
                }

                boolean matches = true;
                for (int i = 0; i < groupParts.length; ++i) {
                    String up = url.reverseParts.get(groupParts.length + 1 - i);
                    if (!up.equals(groupParts[i])) {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    try {
                        return new ResolvedArtifact(groupId, artifactId, matcher.group(1), classifier, url.path);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                }
            }
        }
        throw new RuntimeException("Could not resolve artifact " + groupId + ":" + artifactId + ":" + classifier);
    }

    static class StoredUrl {
        final Path path;
        final String fileName;
        final List<String> reverseParts;

        private StoredUrl(Path path, String fileName, List<String> reverseParts) {
            this.path = path;
            this.fileName = fileName;
            this.reverseParts = reverseParts;
        }
    }
}
