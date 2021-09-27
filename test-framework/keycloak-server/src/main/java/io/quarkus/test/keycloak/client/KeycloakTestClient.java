package io.quarkus.test.keycloak.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.microprofile.config.ConfigProvider;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.util.JsonSerialization;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.common.DevServicesContext;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

public class KeycloakTestClient implements DevServicesContext.ContextAware {

    private final static String AUTH_SERVER_URL_PROP = "quarkus.oidc.auth-server-url";
    private final static String CLIENT_ID_PROP = "quarkus.oidc.client-id";
    private final static String CLIENT_SECRET_PROP = "quarkus.oidc.credentials.secret";

    static {
        RestAssured.useRelaxedHTTPSValidation();
    }

    private DevServicesContext testContext;

    public KeycloakTestClient() {

    }

    /**
     * Get an access token using a password grant with a provided user name.
     * User secret will be the same as the user name, client id will be set to 'quarkus-app' and client secret to 'secret'.
     */
    public String getAccessToken(String userName) {
        return getAccessToken(userName, getClientId());
    }

    /**
     * Get an access token using a password grant with the provided user name and client id.
     * User secret will be the same as the user name, client secret will be set to 'secret'.
     */
    public String getAccessToken(String userName, String clientId) {
        return getAccessToken(userName, userName, clientId);
    }

    /**
     * Get an access token using a password grant with the provided user name, user secret and client id.
     * Client secret will be set to 'secret'.
     */
    public String getAccessToken(String userName, String userSecret, String clientId) {
        return getAccessToken(userName, userSecret, clientId, getClientSecret());
    }

    /**
     * Get an access token using a password grant with the provided user name, user secret, client id and secret.
     * Set the client secret to an empty string or null if it is not required.
     */
    public String getAccessToken(String userName, String userSecret, String clientId, String clientSecret) {
        return getAccessTokenInternal(userName, userSecret, clientId, clientSecret, getAuthServerUrl());
    }

    private String getAccessTokenInternal(String userName, String userSecret, String clientId, String clientSecret,
            String authServerUrl) {
        RequestSpecification requestSpec = RestAssured.given().param("grant_type", "password")
                .param("username", userName)
                .param("password", userSecret)
                .param("client_id", clientId);
        if (clientSecret != null && !clientSecret.isBlank()) {
            requestSpec = requestSpec.param("client_secret", clientSecret);
        }
        return requestSpec.when().post(authServerUrl + "/protocol/openid-connect/token")
                .as(AccessTokenResponse.class).getToken();
    }

    private String getClientId() {
        return getPropertyValue(CLIENT_ID_PROP, "quarkus-app");
    }

    private String getClientSecret() {
        return getPropertyValue(CLIENT_SECRET_PROP, "secret");
    }

    /**
     * Get an admin access token which can be used to create Keycloak realms and perform other Keycloak administration tasks.
     */
    public String getAdminAccessToken() {
        return getAccessTokenInternal("admin", "admin", "admin-cli", null, getAuthServerBaseUrl() + "/realms/master");
    }

    /**
     * Return URL string pointing to a Keycloak base endpoint.
     * For example: 'http://localhost:8081/auth'.
     */
    public String getAuthServerBaseUrl() {
        try {
            var uri = new URI(getAuthServerUrl());
            return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), "/auth", null, null)
                    .toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return URL string pointing to a Keycloak authentication endpoint configured with a 'quarkus.oidc.auth-server'
     * property.
     * For example: 'http://localhost:8081/auth/realms/quarkus'.
     */
    public String getAuthServerUrl() {
        String authServerUrl = getPropertyValue(AUTH_SERVER_URL_PROP, null);
        if (authServerUrl == null) {
            throw new ConfigurationException(AUTH_SERVER_URL_PROP + " is not configured");
        }
        return authServerUrl;
    }

    /**
     * Create a realm.
     */
    public void createRealm(RealmRepresentation realm) {
        try {
            RestAssured
                    .given()
                    .auth().oauth2(getAdminAccessToken())
                    .contentType("application/json")
                    .body(JsonSerialization.writeValueAsBytes(realm))
                    .when()
                    .post(getAuthServerBaseUrl() + "/admin/realms").then()
                    .statusCode(201);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Delete a realm
     */
    public void deleteRealm(String realm) {
        RestAssured
                .given()
                .auth().oauth2(getAdminAccessToken())
                .when()
                .delete(getAuthServerBaseUrl() + "/admin/realms/" + realm).then().statusCode(204);
    }

    private String getPropertyValue(String prop, String defaultValue) {
        return ConfigProvider.getConfig().getOptionalValue(prop, String.class)
                .orElseGet(() -> getDevProperty(prop, defaultValue));
    }

    private String getDevProperty(String prop, String defaultValue) {
        String value = testContext == null ? null : testContext.devServicesProperties().get(prop);
        return value == null ? defaultValue : value;
    }

    @Override
    public void setIntegrationTestContext(DevServicesContext context) {
        this.testContext = context;
    }
}
