package io.quarkus.registry.catalog.json;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quarkus.registry.catalog.PlatformReleaseVersion;
import java.util.Objects;

@JsonSerialize(using = JsonPlatformReleaseVersionSerializer.class)
@JsonDeserialize(using = JsonPlatformReleaseVersionDeserializer.class)
public class JsonPlatformReleaseVersion implements PlatformReleaseVersion {

    public static JsonPlatformReleaseVersion fromString(String s) {
        return new JsonPlatformReleaseVersion(s);
    }

    private final String version;

    private JsonPlatformReleaseVersion(String version) {
        this.version = Objects.requireNonNull(version);
    }

    @Override
    public int compareTo(PlatformReleaseVersion o) {
        if (o instanceof JsonPlatformReleaseVersion) {
            return version.compareTo(((JsonPlatformReleaseVersion) o).version);
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JsonPlatformReleaseVersion that = (JsonPlatformReleaseVersion) o;
        return Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version);
    }

    @Override
    public String toString() {
        return version;
    }
}
