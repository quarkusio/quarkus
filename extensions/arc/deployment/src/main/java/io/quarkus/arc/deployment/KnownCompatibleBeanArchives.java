package io.quarkus.arc.deployment;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.maven.dependency.ArtifactKey;

final class KnownCompatibleBeanArchives {

    private final Map<KnownCompatibleBeanArchiveBuildItem.Reason, Set<ArtifactKey>> compatArchivesByReason;

    KnownCompatibleBeanArchives(List<KnownCompatibleBeanArchiveBuildItem> list) {
        Map<KnownCompatibleBeanArchiveBuildItem.Reason, Set<ArtifactKey>> allCompatArchivesMap = new EnumMap<>(
                KnownCompatibleBeanArchiveBuildItem.Reason.class);
        for (KnownCompatibleBeanArchiveBuildItem item : list) {
            for (KnownCompatibleBeanArchiveBuildItem.Reason reason : item.reasons) {
                allCompatArchivesMap.computeIfAbsent(reason, unused -> new HashSet<>())
                        .add(ArtifactKey.of(item.groupId, item.artifactId, item.classifier, item.type));
            }
        }
        this.compatArchivesByReason = allCompatArchivesMap;
    }

    boolean isKnownCompatible(ArtifactKey artifactKey, KnownCompatibleBeanArchiveBuildItem.Reason reason) {
        if (artifactKey == null) {
            return false;
        }
        Set<ArtifactKey> archives = compatArchivesByReason.get(reason);
        return archives != null && archives.contains(artifactKey);
    }
}
