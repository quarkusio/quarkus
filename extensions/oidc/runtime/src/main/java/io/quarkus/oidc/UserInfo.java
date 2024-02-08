package io.quarkus.oidc;

import jakarta.json.JsonObject;

import org.eclipse.microprofile.jwt.Claims;

import io.quarkus.oidc.common.runtime.AbstractJsonObject;

public class UserInfo extends AbstractJsonObject {

    private static final String EMAIL = "email";
    private static final String NAME = "name";
    private static final String FIRST_NAME = "first_name";
    private static final String FAMILY_NAME = "family_name";
    private static final String DISPLAY_NAME = "display_name";

    public UserInfo() {
    }

    public UserInfo(String userInfoJson) {
        super(userInfoJson);
    }

    public UserInfo(JsonObject json) {
        super(json);
    }

    public String getUserInfoString() {
        return getJsonString();
    }

    public String getName() {
        return getString(NAME);
    }

    public String getFirstName() {
        return getString(FIRST_NAME);
    }

    public String getFamilyName() {
        return getString(FAMILY_NAME);
    }

    public String getDisplayName() {
        return getString(DISPLAY_NAME);
    }

    public String getPreferredUserName() {
        return getString(Claims.preferred_username.name());
    }

    public String getSubject() {
        return getString(Claims.sub.name());
    }

    public String getEmail() {
        return getString(EMAIL);
    }
}
