package io.quarkus.vertx.http.runtime.devmode;

public class AdditionalRouteDescription implements Comparable<AdditionalRouteDescription> {
    private String uri;
    private String description;

    // for bytecode recorder
    public AdditionalRouteDescription() {
    }

    public AdditionalRouteDescription(String uri, String description) {
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
    public int compareTo(AdditionalRouteDescription o) {
        return uri.compareTo(o.uri);
    }
}
