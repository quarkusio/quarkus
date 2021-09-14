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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        JsonPlatformReleaseVersion other = (JsonPlatformReleaseVersion) obj;
        if (version == null) {
            if (other.version != null)
                return false;
        } else if (!version.equals(other.version))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return version;
    }
}
