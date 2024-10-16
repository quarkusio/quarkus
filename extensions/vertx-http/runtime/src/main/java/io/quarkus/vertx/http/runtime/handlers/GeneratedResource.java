package io.quarkus.vertx.http.runtime.handlers;

import java.util.Objects;

public class GeneratedResource {

    private String publicPath;
    private byte[] content;

    public GeneratedResource(String publicPath, byte[] content) {
        this.publicPath = publicPath;
        this.content = content;
    }

    public String getPublicPath() {
        return publicPath;
    }

    public byte[] getContent() {
        return content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        GeneratedResource that = (GeneratedResource) o;
        return Objects.equals(publicPath, that.publicPath);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(publicPath);
    }
}
