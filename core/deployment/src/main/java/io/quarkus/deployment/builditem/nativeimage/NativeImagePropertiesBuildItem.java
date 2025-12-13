package io.quarkus.deployment.builditem.nativeimage;

import java.util.List;
import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that represents parsed META-INF/native-image/native-image.properties content.
 * This contains the native-image command line arguments that were specified in the properties file.
 */
public final class NativeImagePropertiesBuildItem extends MultiBuildItem {

    private final String sourceJar;
    private final String sourceResource;
    private final List<String> args;

    public NativeImagePropertiesBuildItem(String sourceJar, String sourceResource, List<String> args) {
        this.sourceJar = sourceJar;
        this.sourceResource = sourceResource;
        this.args = List.copyOf(args);
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
     * @return the parsed command line arguments from the native-image.properties file
     */
    public List<String> getArgs() {
        return args;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        NativeImagePropertiesBuildItem that = (NativeImagePropertiesBuildItem) o;
        return Objects.equals(sourceJar, that.sourceJar) &&
                Objects.equals(sourceResource, that.sourceResource) &&
                Objects.equals(args, that.args);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceJar, sourceResource, args);
    }

    @Override
    public String toString() {
        return "NativeImagePropertiesBuildItem{" +
                "sourceJar='" + sourceJar + '\'' +
                ", sourceResource='" + sourceResource + '\'' +
                ", args=" + args +
                '}';
    }
}