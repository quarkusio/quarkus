package io.quarkus.registry.catalog;

import java.io.IOException;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Representation of Quarkus Platform release.
 *
 * @see #fromString(String) to create a version from a String
 * @see PlatformReleaseVersion.Serializer
 * @see PlatformReleaseVersion.Deserializer
 */
@JsonSerialize(using = PlatformReleaseVersion.Serializer.class)
@JsonDeserialize(using = PlatformReleaseVersion.Deserializer.class)
public interface PlatformReleaseVersion extends Comparable<PlatformReleaseVersion> {

    static PlatformReleaseVersion fromString(String s) {
        return new VersionImpl(s);
    }

    @JsonSerialize(using = PlatformReleaseVersion.Serializer.class)
    @JsonDeserialize(using = PlatformReleaseVersion.Deserializer.class)
    class VersionImpl implements PlatformReleaseVersion {

        private final String version;

        private VersionImpl(String version) {
            this.version = Objects.requireNonNull(version);
        }

        @Override
        public int compareTo(PlatformReleaseVersion o) {
            if (o instanceof VersionImpl) {
                return version.compareTo(((VersionImpl) o).version);
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
            VersionImpl that = (VersionImpl) o;
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

    class Deserializer extends JsonDeserializer<PlatformReleaseVersion> {
        @Override
        public PlatformReleaseVersion deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException {
            return PlatformReleaseVersion.fromString(p.getText());
        }
    }

    class Serializer extends JsonSerializer<VersionImpl> {
        @Override
        public void serialize(VersionImpl value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeString(value.toString());
        }
    }
}
