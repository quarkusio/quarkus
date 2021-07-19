package io.quarkus.registry.catalog.selection;

import io.quarkus.maven.ArtifactKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class OriginCombination {

    private final OriginWithPreference[] collectedOrigins;

    public OriginCombination() {
        collectedOrigins = new OriginWithPreference[0];
    }

    private OriginCombination(OriginWithPreference[] selectedOrigins) {
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

    public Collection<OriginWithPreference> getUniqueSortedOrigins() {
        if (collectedOrigins.length == 0) {
            return Collections.emptyList();
        }
        if (collectedOrigins.length == 1) {
            return Collections.singletonList(collectedOrigins[0]);
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

    public int size() {
        return collectedOrigins.length;
    }

    private static <T> T[] addLast(T[] arr, T item) {
        final T[] copy = Arrays.copyOf(arr, arr.length + 1);
        copy[copy.length - 1] = item;
        return copy;
    }
}
