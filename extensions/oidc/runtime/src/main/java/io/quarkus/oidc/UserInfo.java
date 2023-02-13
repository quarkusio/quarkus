package io.quarkus.oidc;

import jakarta.json.JsonObject;

import io.quarkus.oidc.runtime.AbstractJsonObjectResponse;

public class UserInfo extends AbstractJsonObjectResponse {

    public UserInfo() {
    }

    public UserInfo(String userInfoJson) {
        super(userInfoJson);
    }

    public UserInfo(JsonObject json) {
        super(json);
    }

    public String getUserInfoString() {
        return getNonNullJsonString();
    }
}
