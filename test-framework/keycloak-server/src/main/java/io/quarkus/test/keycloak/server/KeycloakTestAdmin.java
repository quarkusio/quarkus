package io.quarkus.test.keycloak.server;

import org.eclipse.microprofile.config.ConfigProvider;
import org.keycloak.representations.AccessTokenResponse;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.RestAssured;

public class KeycloakTestAdmin {

    private final static String AUTH_SERVER_URL_PROP = "quarkus.oidc.auth-server-url";
    private final static String CLIENT_ID_PROP = "quarkus.oidc.client-id";
    private final static String CLIENT_SECRET_PROP = "quarkus.oidc.credentials.secret";

    static {
        RestAssured.useRelaxedHTTPSValidation();
    }

    private QuarkusIntegrationTest.Context testContext;

    public KeycloakTestAdmin() {

    }

    public KeycloakTestAdmin(QuarkusIntegrationTest.Context testContext) {
        this.testContext = testContext;
    }

    public String getAccessToken(String userName) {
        return getAccessToken(userName, getClientId());
    }

    public String getAccessToken(String userName, String clientId) {
        return getAccessToken(userName, userName, clientId);
    }

    public String getAccessToken(String userName, String userSecret, String clientId) {
        return getAccessToken(userName, userSecret, clientId, getClientSecret());
    }

    public String getAccessToken(String userName, String userSecret, String clientId, String clientSecret) {
        return RestAssured.given().param("grant_type", "password")
                .param("username", userName)
                .param("password", userSecret)
                .param("client_id", clientId)
                .param("client_secret", clientSecret)
                .when()
                .post(getAuthServerUrl() + "/protocol/openid-connect/token")
                .as(AccessTokenResponse.class).getToken();
    }

    private String getClientId() {
        return getPropertyValue(CLIENT_ID_PROP, "quarkus-app");
    }

    private String getClientSecret() {
        return getPropertyValue(CLIENT_SECRET_PROP, "secret");
    }

    private String getAuthServerUrl() {
        String authServerUrl = getPropertyValue(AUTH_SERVER_URL_PROP, null);
        if (authServerUrl == null) {
            throw new ConfigurationException(AUTH_SERVER_URL_PROP + " is not configured");
        }
        return authServerUrl;
    }

    private String getPropertyValue(String prop, String defaultValue) {
        return ConfigProvider.getConfig().getOptionalValue(prop, String.class)
                .orElseGet(() -> getDevProperty(prop, defaultValue));
    }

    private String getDevProperty(String prop, String defaultValue) {
        String value = testContext == null ? null : testContext.devServicesProperties().get(prop);
        return value == null ? defaultValue : value;
    }

}
