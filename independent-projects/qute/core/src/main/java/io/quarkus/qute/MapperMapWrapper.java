package io.quarkus.qute;

import java.util.Map;
import java.util.Set;

final class MapperMapWrapper implements Mapper {

    private final Map<String, ?> map;

    MapperMapWrapper(Map<String, ?> map) {
        this.map = map;
    }

    @Override
    public boolean appliesTo(String key) {
        return map.containsKey(key);
    }

    @Override
    public Object get(String key) {
        return map.get(key);
    }

    @Override
    public Set<String> mappedKeys() {
        return map.keySet();
    }

}
