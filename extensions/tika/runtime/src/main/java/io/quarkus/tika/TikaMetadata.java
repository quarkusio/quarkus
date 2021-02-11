package io.quarkus.tika;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TikaMetadata {

    private Map<String, List<String>> metadata;
    private Set<String> metadataKeys;

    public TikaMetadata(Map<String, List<String>> metadata) {
        this.metadata = metadata;
        this.metadataKeys = Collections.unmodifiableSet(metadata.keySet());
    }

    public Set<String> getNames() {
        return metadataKeys;
    }

    public List<String> getValues(String name) {
        return metadata.containsKey(name) ? Collections.unmodifiableList(metadata.get(name)) : null;
    }

    public String getSingleValue(String name) {
        List<String> values = metadata.get(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }
}
