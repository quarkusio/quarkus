package io.quarkus.builder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.builder.item.MultiBuildItem;

public class MultiBuildItemsTest {
    @Test
    public void testMultis() {

        final ItemId id1 = new ItemId(Item1.class);
        final ItemId id2 = new ItemId(Item2.class);
        final ItemId idC1 = new ItemId(ComparableItem1.class);
        final ItemId idC2 = new ItemId(ComparableItem2.class);

        Map<ItemId, int[]> producingOrdinals = new HashMap<ItemId, int[]>();
        producingOrdinals.put(id1, new int[] { 2, 4 });

        final MultiBuildItems multis = new MultiBuildItems(producingOrdinals);
        multis.putInitial(id1, new Item1("i1.-1.1"));
        multis.putInitial(id1, new Item1("i1.-1.2"));

        multis.putInitial(idC1, new ComparableItem1("c1.2"));
        multis.putInitial(idC1, new ComparableItem1("c1.1"));

        {
            final List<Item1> actual = multis.get(id1);
            Assertions.assertEquals(Arrays.asList("i1.-1.1", "i1.-1.2"),
                    actual.stream().map(Item1::toString).collect(Collectors.toList()));
        }

        {
            final List<ComparableItem1> actual = multis.get(idC1);
            Assertions.assertEquals(Arrays.asList("c1.2", "c1.1"),
                    actual.stream().map(ComparableItem1::toString).collect(Collectors.toList()));
        }

        {
            final List<Item2> actual = multis.get(id2);
            Assertions.assertEquals(Arrays.asList(),
                    actual.stream().map(Item2::toString).collect(Collectors.toList()));
        }

        {
            final List<ComparableItem2> actual = multis.get(idC2);
            Assertions.assertEquals(Arrays.asList(),
                    actual.stream().map(ComparableItem2::toString).collect(Collectors.toList()));
        }

        multis.put(4, id1, new Item1("i1.4.1"));
        multis.put(2, id1, new Item1("i1.2.1"));
        multis.put(4, id1, new Item1("i1.4.2"));
        multis.put(2, id1, new Item1("i1.2.2"));

        {
            final List<Item1> actual = multis.get(id1);
            Assertions.assertEquals(Arrays.asList("i1.-1.1", "i1.-1.2", "i1.2.1", "i1.2.2", "i1.4.1", "i1.4.2"),
                    actual.stream().map(Item1::toString).collect(Collectors.toList()));
        }

    }

    public static final class Item1 extends MultiBuildItem {
        private final String name;

        public Item1(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static final class Item2 extends MultiBuildItem {
        private final String name;

        public Item2(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static final class ComparableItem1 extends MultiBuildItem implements Comparable<ComparableItem1> {
        private final String name;

        public ComparableItem1(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public int compareTo(ComparableItem1 other) {
            return this.name.compareTo(other.name);
        }
    }

    public static final class ComparableItem2 extends MultiBuildItem implements Comparable<ComparableItem1> {
        private final String name;

        public ComparableItem2(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public int compareTo(ComparableItem1 other) {
            return this.name.compareTo(other.name);
        }
    }
}
