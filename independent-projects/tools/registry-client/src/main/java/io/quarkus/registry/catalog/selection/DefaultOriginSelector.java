package io.quarkus.registry.catalog.selection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import io.quarkus.maven.dependency.ArtifactKey;

public class DefaultOriginSelector implements OriginSelector {

    private OriginCombination recommended;
    private double recommendedScore = -1;
    private int recommendedRegistryPreference = -1;

    public DefaultOriginSelector(Collection<ExtensionOrigins> extOrigins) {
        final List<ArtifactKey> extKeys = new ArrayList<>(extOrigins.size());
        for (ExtensionOrigins eo : extOrigins) {
            extKeys.add(eo.getExtensionKey());
        }
        int highestRegistryPreference = 0;
        final TreeMap<OriginPreference, WorkingCombination> map = new TreeMap<>();
        for (ExtensionOrigins eo : extOrigins) {
            for (OriginWithPreference op : eo.getOrigins()) {
                map.computeIfAbsent(op.getPreference(), k -> new WorkingCombination(k, extKeys))
                        .addExtension(eo.getExtensionKey(), op);
                highestRegistryPreference = Math.max(op.getPreference().registryPreference, highestRegistryPreference);
            }
        }
        final List<WorkingCombination> list = new ArrayList<>(map.values());
        for (int i = 0; i < list.size(); ++i) {
            WorkingCombination combination = list.get(i);
            if (recommendedRegistryPreference >= 0
                    && combination.preferences.get(0).registryPreference > recommendedRegistryPreference) {
                break;
            }
            if (combination.isComplete()) {
                evaluateCandidate(extOrigins, highestRegistryPreference, combination);
                break;
            }
            combination = complete(combination, list, i + 1);
            if (combination != null) {
                evaluateCandidate(extOrigins, highestRegistryPreference, combination);
            }
        }
    }

    private void evaluateCandidate(Collection<ExtensionOrigins> extOrigins, int highestRegistryPreference,
            WorkingCombination combination) {
        if (recommended != null) {
            if (recommendedScore < 0) {
                recommendedScore = OriginCombination.calculateScore(recommended, highestRegistryPreference, extOrigins.size());
            }
            final OriginCombination candidate = combination.toCombination();
            final double candidateScore = OriginCombination.calculateScore(candidate, highestRegistryPreference,
                    extOrigins.size());
            if (recommendedScore < candidateScore) {
                recommended = candidate;
                recommendedScore = candidateScore;
            }
        } else {
            recommended = combination.toCombination();
            recommendedRegistryPreference = combination.preferences.get(0).registryPreference;
        }
    }

    private static WorkingCombination complete(WorkingCombination combination, List<WorkingCombination> list, int fromIndex) {
        for (int i = fromIndex; i < list.size(); ++i) {
            final WorkingCombination candidate = list.get(i);
            if (!combination.canBeCombinedWith(candidate.preferences)) {
                continue;
            }
            WorkingCombination augmented = combination.addMissing(candidate);
            if (augmented == null) {
                continue;
            }
            if (augmented.isComplete()) {
                return augmented;
            }
            augmented = complete(augmented, list, i + 1);
            if (augmented != null) {
                return augmented;
            }
        }
        return null;
    }

    @Override
    public OriginCombination calculateRecommendedCombination() {
        return recommended;
    }

    private static class WorkingCombination {
        final List<OriginPreference> preferences;
        final Set<ArtifactKey> missingExtensions;
        final Map<ArtifactKey, OriginWithPreference> extensions;

        private WorkingCombination(OriginPreference preference, Collection<ArtifactKey> allExtKeys) {
            this.preferences = Collections.singletonList(preference);
            this.missingExtensions = new HashSet<>(allExtKeys);
            this.extensions = new HashMap<>();
        }

        private WorkingCombination(WorkingCombination original) {
            preferences = new ArrayList<>(original.preferences);
            missingExtensions = new HashSet<>(original.missingExtensions);
            extensions = new HashMap<>(original.extensions);
        }

        boolean isComplete() {
            return missingExtensions.isEmpty();
        }

        boolean canBeCombinedWith(OriginPreference other) {
            for (OriginPreference p : preferences) {
                if (!p.canBeCombinedWith(other)) {
                    return false;
                }
            }
            return true;
        }

        boolean canBeCombinedWith(Collection<OriginPreference> other) {
            for (OriginPreference o : other) {
                if (!canBeCombinedWith(o)) {
                    return false;
                }
            }
            return true;
        }

        WorkingCombination addMissing(WorkingCombination other) {
            WorkingCombination augmented = null;
            for (ArtifactKey missingKey : missingExtensions) {
                final OriginWithPreference missing = other.extensions.get(missingKey);
                if (missing == null || !canBeCombinedWith(missing.getPreference())) {
                    continue;
                }
                if (augmented == null) {
                    augmented = new WorkingCombination(this);
                }
                augmented.addExtension(missingKey, missing);
            }
            return augmented;
        }

        void addExtension(ArtifactKey ext, OriginWithPreference origin) {
            extensions.put(ext, origin);
            missingExtensions.remove(ext);
        }

        OriginCombination toCombination() {
            return new OriginCombination(extensions.values().toArray(new OriginWithPreference[0]));
        }
    }
}
