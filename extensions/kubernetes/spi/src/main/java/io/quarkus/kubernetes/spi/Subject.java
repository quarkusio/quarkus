package io.quarkus.kubernetes.spi;

public class Subject {
    private final String apiGroup;
    private final String kind;
    private final String name;
    private final String namespace;

    public Subject(String apiGroup, String kind, String name, String namespace) {
        this.apiGroup = apiGroup;
        this.kind = kind;
        this.name = name;
        this.namespace = namespace;
    }

    public String getApiGroup() {
        return apiGroup;
    }

    public String getKind() {
        return kind;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }
}
