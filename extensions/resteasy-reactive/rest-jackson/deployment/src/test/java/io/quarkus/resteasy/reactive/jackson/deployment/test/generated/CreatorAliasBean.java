package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CreatorAliasBean {

    private final String name;
    private final String code;

    @JsonCreator
    public CreatorAliasBean(
            @JsonProperty("name") @JsonAlias({ "fullName", "display_name" }) String name,
            @JsonProperty("code") @JsonAlias("identifier") String code) {
        this.name = name;
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }
}
