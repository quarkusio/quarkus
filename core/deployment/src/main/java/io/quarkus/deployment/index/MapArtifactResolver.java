package io.quarkus.deployment.index;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MapArtifactResolver implements ArtifactResolver {

    private final Map<Key, ResolvedArtifact> artifacts;

    public MapArtifactResolver(List<ResolvedArtifact> artifacts) {
        Map<Key, ResolvedArtifact> map = new HashMap<>();
        for (ResolvedArtifact i : artifacts) {
            map.put(new Key(i.getGroupId(), i.getArtifactId(), i.getClassifier()), i);
        }
        this.artifacts = map;
    }

    @Override
    public ResolvedArtifact getArtifact(String groupId, String artifactId, String classifier) {
        return artifacts.get(new Key(groupId, artifactId, classifier));
    }

    private static class Key {
        final String groupId;
        final String artifactId;
        final String classifier;

        private Key(String groupId, String artifactId, String classifier) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.classifier = classifier;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Key key = (Key) o;
            return Objects.equals(groupId, key.groupId) &&
                    Objects.equals(artifactId, key.artifactId) &&
                    Objects.equals(classifier, key.classifier);
        }

        @Override
        public int hashCode() {

            return Objects.hash(groupId, artifactId, classifier);
        }
    }

}
