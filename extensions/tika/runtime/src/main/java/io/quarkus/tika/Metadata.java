package io.quarkus.tika;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Metadata {

    private Map<String, List<String>> map;

    public Metadata(Map<String, List<String>> map) {
        this.map = map;
    }

    public Set<String> getNames() {
        return Collections.unmodifiableSet(map.keySet());
    }

    public List<String> getValues(String name) {
        return map.containsKey(name) ? Collections.unmodifiableList(map.get(name)) : null;
    }

    public String getSingleValue(String name) {
        List<String> values = getValues(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }
}
