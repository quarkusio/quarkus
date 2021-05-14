package io.quarkus.vault.runtime.client.dto.transit;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultTransitKeyVersionData implements VaultModel {

    public String name;
    @JsonProperty("creation_time")
    public OffsetDateTime creationTime;
    @JsonProperty("public_key")
    public String publicKey;

    static class Deserializer extends JsonDeserializer<VaultTransitKeyVersionData> {

        Deserializer() {
        }

        @Override
        public VaultTransitKeyVersionData deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (p.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                long timestamp = p.readValueAs(Long.class);
                VaultTransitKeyVersionData data = new VaultTransitKeyVersionData();
                data.creationTime = Instant.ofEpochSecond(timestamp).atOffset(ZoneOffset.UTC);
                return data;
            } else {
                return p.readValueAs(VaultTransitKeyVersionData.class);
            }
        }
    }
}
