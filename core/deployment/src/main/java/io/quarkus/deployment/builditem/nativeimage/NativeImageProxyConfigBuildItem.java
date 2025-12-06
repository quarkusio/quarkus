package io.quarkus.deployment.builditem.nativeimage;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that represents parsed META-INF/native-image/proxy-config.json content.
 * This contains the raw JSON configuration that was specified in the proxy-config.json file.
 */
public final class NativeImageProxyConfigBuildItem extends MultiBuildItem {

    private final String sourceJar;
    private final String sourceResource;
    private final String jsonContent;

    public NativeImageProxyConfigBuildItem(String sourceJar, String sourceResource, String jsonContent) {
        this.sourceJar = sourceJar;
        this.sourceResource = sourceResource;
        this.jsonContent = jsonContent;
    }

    /**
     * @return the name of the JAR file that contained this metadata
     */
    public String getSourceJar() {
        return sourceJar;
    }

    /**
     * @return the resource path within the JAR that contained this metadata
     */
    public String getSourceResource() {
        return sourceResource;
    }

    /**
     * @return the raw JSON content from the proxy-config.json file
     */
    public String getJsonContent() {
        return jsonContent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        NativeImageProxyConfigBuildItem that = (NativeImageProxyConfigBuildItem) o;
        return Objects.equals(sourceJar, that.sourceJar) &&
                Objects.equals(sourceResource, that.sourceResource) &&
                Objects.equals(jsonContent, that.jsonContent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceJar, sourceResource, jsonContent);
    }

    @Override
    public String toString() {
        return "NativeImageProxyConfigBuildItem{" +
                "sourceJar='" + sourceJar + '\'' +
                ", sourceResource='" + sourceResource + '\'' +
                ", jsonContent='" + jsonContent + '\'' +
                '}';
    }
}