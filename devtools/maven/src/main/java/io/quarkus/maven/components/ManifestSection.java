package io.quarkus.maven.components;

import java.util.LinkedHashMap;
import java.util.Map;

public class ManifestSection {

    private String name = null;
    private Map<String, String> manifestEntries = new LinkedHashMap<>();

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setManifestEntries(Map<String, String> manifestEntries) {
        this.manifestEntries = manifestEntries;
    }

    public Map<String, String> getManifestEntries() {
        return manifestEntries;
    }

}
