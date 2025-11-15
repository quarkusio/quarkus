package io.quarkus.arc.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Grouping {
    @FunctionalInterface
    public interface Builder<E, G> {
        G apply(int id, List<E> elements);
    }

    public static <E, G> List<G> of(Collection<E> elements, int groupLimit, Builder<E, G> builder) {
        List<G> groups = new ArrayList<>(elements.size() / groupLimit + 1);
        List<E> elementsInGroup = new ArrayList<>(groupLimit);
        for (E element : elements) {
            elementsInGroup.add(element);

            if (elementsInGroup.size() == groupLimit) {
                groups.add(builder.apply(groups.size(), List.copyOf(elementsInGroup)));
                elementsInGroup.clear();
            }
        }
        if (!elementsInGroup.isEmpty()) {
            groups.add(builder.apply(groups.size(), List.copyOf(elementsInGroup)));
        }
        return List.copyOf(groups);
    }
}
