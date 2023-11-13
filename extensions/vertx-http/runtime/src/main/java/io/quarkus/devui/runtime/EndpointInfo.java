package io.quarkus.devui.runtime;

public class EndpointInfo implements Comparable<EndpointInfo> {
    private String uri;
    private String description;

    // for bytecode recorder
    public EndpointInfo() {
    }

    public EndpointInfo(String uri, String description) {
        this.uri = uri;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getUri() {
        return uri;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public int compareTo(EndpointInfo o) {
        return uri.compareTo(o.uri);
    }
}
