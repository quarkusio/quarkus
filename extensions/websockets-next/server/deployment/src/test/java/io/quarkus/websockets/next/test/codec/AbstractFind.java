package io.quarkus.websockets.next.test.codec;

import java.util.Comparator;
import java.util.List;

public abstract class AbstractFind {

    Item find(List<Item> items) {
        return items.stream().sorted(Comparator.comparingInt(Item::getCount)).findFirst().orElse(null);
    }

}
