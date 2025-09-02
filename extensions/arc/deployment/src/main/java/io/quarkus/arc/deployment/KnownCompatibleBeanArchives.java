package io.quarkus.arc.deployment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.maven.dependency.ArtifactKey;

final class KnownCompatibleBeanArchives {
    static class Key {
        final String groupId;
        final String artifactId;
        final String classifier;

        Key(String groupId, String artifactId, String classifier) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.classifier = classifier;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key)) {
                return false;
            }
            Key key = (Key) o;
            return Objects.equals(groupId, key.groupId)
                    && Objects.equals(artifactId, key.artifactId)
                    && Objects.equals(classifier, key.classifier);
        }

        @Override
        public int hashCode() {
            return Objects.hash(groupId, artifactId, classifier);
        }
    }

    private final Map<KnownCompatibleBeanArchiveBuildItem.Reason, Set<Key>> compatArchivesByReason;

    KnownCompatibleBeanArchives(List<KnownCompatibleBeanArchiveBuildItem> list) {
        Map<KnownCompatibleBeanArchiveBuildItem.Reason, Set<Key>> allCompatArchivesMap = new HashMap<>();
        for (KnownCompatibleBeanArchiveBuildItem item : list) {
            for (KnownCompatibleBeanArchiveBuildItem.Reason reason : item.reasons) {
                allCompatArchivesMap.computeIfAbsent(reason, unused -> new HashSet<>())
                        .add(new Key(item.groupId, item.artifactId, item.classifier));
            }
        }
        this.compatArchivesByReason = allCompatArchivesMap;
    }

    boolean isKnownCompatible(ApplicationArchive archive, KnownCompatibleBeanArchiveBuildItem.Reason reason) {
        ArtifactKey artifact = archive.getKey();
        if (artifact == null) {
            return false;
        }

        Set<Key> archives = compatArchivesByReason.get(reason);
        return archives == null ? false
                : archives
                        .contains(new Key(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier()));
    }
}
