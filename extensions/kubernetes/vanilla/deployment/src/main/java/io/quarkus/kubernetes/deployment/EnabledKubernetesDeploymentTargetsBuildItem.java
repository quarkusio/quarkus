package io.quarkus.kubernetes.deployment;

import java.util.List;

import io.quarkus.builder.item.SimpleBuildItem;

public final class EnabledKubernetesDeploymentTargetsBuildItem extends SimpleBuildItem {

    private final List<Entry> entriesSortedByPriority;

    public EnabledKubernetesDeploymentTargetsBuildItem(List<Entry> entriesSortedByPriority) {
        if (entriesSortedByPriority.isEmpty()) {
            throw new IllegalArgumentException("At least one enabled entry must be active");
        }
        this.entriesSortedByPriority = entriesSortedByPriority;
    }

    public List<Entry> getEntriesSortedByPriority() {
        return entriesSortedByPriority;
    }

    public static class Entry {
        private final String name;
        private final String kind;
        private final int priority;

        public Entry(String name, String kind, int priority) {
            this.name = name;
            this.kind = kind;
            this.priority = priority;
        }

        public String getName() {
            return name;
        }

        public String getKind() {
            return kind;
        }

        public int getPriority() {
            return priority;
        }
    }
}
