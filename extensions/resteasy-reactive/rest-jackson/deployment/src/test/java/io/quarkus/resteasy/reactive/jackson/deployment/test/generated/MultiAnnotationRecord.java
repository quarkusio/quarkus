package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MultiAnnotationRecord(
        @JsonProperty("title") String name,
        @JsonAlias( {
                "desc", "description" }) String summary,
        @JsonProperty("is_active") boolean active){
}
