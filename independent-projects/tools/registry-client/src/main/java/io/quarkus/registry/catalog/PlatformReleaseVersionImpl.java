package io.quarkus.registry.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;
import java.util.Objects;

@JsonSerialize(using = PlatformReleaseVersionImpl.Serializer.class)
@JsonDeserialize(using = PlatformReleaseVersionImpl.Deserializer.class)
public class PlatformReleaseVersionImpl implements PlatformReleaseVersion {
    public static PlatformReleaseVersionImpl fromString(String s) {
        return new PlatformReleaseVersionImpl(s);
    }

    private final String version;

    private PlatformReleaseVersionImpl(String version) {
        this.version = Objects.requireNonNull(version);
    }

    @JsonIgnore
    public String getVersion() {
        // TODO: Remove?
        return version;
    }

    @Override
    public int compareTo(PlatformReleaseVersion o) {
        return version.compareTo(o.getVersion());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof PlatformReleaseVersion)) {
            return false;
        }
        PlatformReleaseVersion that = (PlatformReleaseVersion) o;
        return Objects.equals(version, that.getVersion());
    }

    @Override
    public int hashCode() {
        return Objects.hash(version);
    }

    @Override
    public String toString() {
        return version;
    }

    public static class Deserializer extends JsonDeserializer<PlatformReleaseVersionImpl> {
        @Override
        public PlatformReleaseVersionImpl deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException, JsonProcessingException {
            return PlatformReleaseVersionImpl.fromString(p.getText());
        }
    }

    public static class Serializer extends JsonSerializer<PlatformReleaseVersionImpl> {
        @Override
        public void serialize(PlatformReleaseVersionImpl value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeString(value.toString());
        }
    }
}
