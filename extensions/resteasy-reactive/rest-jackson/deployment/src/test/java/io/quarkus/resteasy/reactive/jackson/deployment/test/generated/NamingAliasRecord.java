package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import com.fasterxml.jackson.annotation.JsonAlias;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record NamingAliasRecord(
        String firstName,
        @JsonAlias( {
                "surname", "familyName" }) String lastName){
}
