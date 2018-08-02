package org.jboss.shamrock.deployment.index;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;

public class ArtifactIndex {

    private final ArtifactResolver resolver;

    public ArtifactIndex(ArtifactResolver resolver) {
        this.resolver = resolver;
    }

    public IndexView getIndex(String groupId, String artifactId, String classifier) {
        ResolvedArtifact artifact = resolver.getArtifact(groupId, artifactId, classifier);
        if (artifact == null) {
            throw new RuntimeException("Unable to resolve artifact " + groupId + ":" + artifactId + ":" + classifier);
        }
        Indexer indexer = new Indexer();
        try {
            try (JarFile file = new JarFile(artifact.getArtifactPath().toFile())) {
                Enumeration<JarEntry> e = file.entries();
                while (e.hasMoreElements()) {
                    JarEntry entry = e.nextElement();
                    if (entry.getName().endsWith(".class")) {
                        try (InputStream inputStream = file.getInputStream(entry)) {
                            indexer.index(inputStream);
                        }
                    }
                }
            }
            return indexer.complete();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
