package io.quarkus.docs.generation;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtensionJson {
    public List<Extension> extensions;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Extension {
        public String name, groupId, artifactId;
    }
}