package io.quarkus.arc.deployment;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.maven.dependency.ArtifactKey;

final class KnownCompatibleBeanArchives {
    private static class Key {
        final String groupId;
        final String artifactId;
        final String classifier;

        Key(String groupId, String artifactId, String classifier) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.classifier = classifier;
        }
    }

    private final Set<Key> keys;

    KnownCompatibleBeanArchives(List<KnownCompatibleBeanArchiveBuildItem> list) {
        Set<Key> keys = new HashSet<>();
        for (KnownCompatibleBeanArchiveBuildItem item : list) {
            keys.add(new Key(item.groupId, item.artifactId, item.classifier));
        }
        this.keys = keys;
    }

    boolean isKnownCompatible(ApplicationArchive archive) {
        ArtifactKey artifact = archive.getKey();
        if (artifact == null) {
            return false;
        }

        return keys.contains(new Key(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier()));
    }
}
