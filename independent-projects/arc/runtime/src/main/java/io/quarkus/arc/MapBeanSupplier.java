package io.quarkus.arc;

import java.util.Map;
import java.util.function.Supplier;

/**
 * {@link Supplier} implementation that supplies a bean from a Map. The key is pre-configured.
 *
 * @author Maarten Mulders
 */
public class MapBeanSupplier implements Supplier {
    private final Map map;
    private final String id;

    public MapBeanSupplier(Map map, String id) {
        this.map = map;
        this.id = id;
    }

    @Override
    public Object get() {
        return map.get(id);
    }
}
