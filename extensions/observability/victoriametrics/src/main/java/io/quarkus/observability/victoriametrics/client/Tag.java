package io.quarkus.observability.victoriametrics.client;

public class Tag {
    private final String name;
    private final String key;

    public Tag(String name, String key) {
        this.name = name;
        this.key = key;
    }

    public static Tag of(String name, String key) {
        return new Tag(name, key);
    }

    public String getName() {
        return name;
    }

    public String getKey() {
        return key;
    }
}
