package io.quarkus.registry.catalog.selection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.registry.catalog.ExtensionCatalog;

public class OriginCombination {

    static double calculateScore(OriginCombination s, int highestRegistryPreference, int originsTotal) {
        double combinationScore = 0;
        for (OriginWithPreference o : s.getCollectedOrigins()) {
            combinationScore += Math.pow(originsTotal,
                    highestRegistryPreference + 1 - o.getPreference().registryPreference)
                    * ((((double) Integer.MAX_VALUE) + 1 - o.getPreference().platformPreference) / Integer.MAX_VALUE);
        }
        return combinationScore;
    }

    private final OriginWithPreference[] collectedOrigins;

    public OriginCombination() {
        collectedOrigins = new OriginWithPreference[0];
    }

    OriginCombination(OriginWithPreference[] selectedOrigins) {
        this.collectedOrigins = selectedOrigins;
    }

    private void sort() {
        Arrays.sort(collectedOrigins, new Comparator<OriginWithPreference>() {
            @Override
            public int compare(OriginWithPreference o1, OriginWithPreference o2) {
                return o1.getPreference().compareTo(o2.getPreference());
            }
        });
    }

    OriginCombination add(ArtifactKey extKey, OriginWithPreference origin) {
        for (OriginWithPreference selectedOrigin : collectedOrigins) {
            if (!selectedOrigin.canBeCombinedWith(origin)) {
                return null;
            }
        }
        return new OriginCombination(addLast(collectedOrigins, origin));
    }

    List<OriginWithPreference> getCollectedOrigins() {
        return Arrays.asList(collectedOrigins);
    }

    public List<OriginWithPreference> getUniqueSortedOrigins() {
        if (collectedOrigins.length == 0) {
            return List.of();
        }
        if (collectedOrigins.length == 1) {
            return List.of(collectedOrigins[0]);
        }
        sort();
        final List<OriginWithPreference> result = new ArrayList<>(collectedOrigins.length);
        result.add(collectedOrigins[0]);
        for (int i = 1; i < collectedOrigins.length; ++i) {
            final OriginWithPreference o = collectedOrigins[i];
            if (!result.get(result.size() - 1).isSameAs(o)) {
                result.add(o);
            }
        }
        return result;
    }

    public List<ExtensionCatalog> getUniqueSortedCatalogs() {
        if (collectedOrigins.length == 0) {
            return List.of();
        }
        if (collectedOrigins.length == 1) {
            return List.of(collectedOrigins[0].getCatalog());
        }
        sort();
        final List<ExtensionCatalog> result = new ArrayList<>(collectedOrigins.length);
        OriginWithPreference prevOrigin = collectedOrigins[0];
        result.add(prevOrigin.getCatalog());
        for (int i = 1; i < collectedOrigins.length; ++i) {
            final OriginWithPreference o = collectedOrigins[i];
            if (!prevOrigin.isSameAs(o)) {
                result.add(o.getCatalog());
                prevOrigin = o;
            }
        }
        return result;
    }

    public int size() {
        return collectedOrigins.length;
    }

    private static <T> T[] addLast(T[] arr, T item) {
        final T[] copy = Arrays.copyOf(arr, arr.length + 1);
        copy[copy.length - 1] = item;
        return copy;
    }
}
