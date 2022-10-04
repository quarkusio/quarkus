package io.quarkus.runtime.logging;

import java.util.Collections;
import java.util.Map;

public class DiscoveredLogComponents {

    private Map<String, String> nameToFilterClass = Collections.emptyMap();

    public Map<String, String> getNameToFilterClass() {
        return nameToFilterClass;
    }

    public void setNameToFilterClass(Map<String, String> nameToFilterClass) {
        this.nameToFilterClass = nameToFilterClass;
    }

    public static DiscoveredLogComponents ofEmpty() {
        return new DiscoveredLogComponents();
    }
}
