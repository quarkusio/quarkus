package io.quarkus.test.keycloak.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

import org.jboss.logging.Logger;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.util.JsonSerialization;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.restassured.RestAssured;

public class KeycloakContainer extends GenericContainer<KeycloakContainer> {

    private static final Logger LOG = Logger.getLogger(KeycloakContainer.class);

    private static final String KEYCLOAK_VERSION = System.getProperty("keycloak.version");
    private static final String KEYCLOAK_DOCKER_IMAGE = System.getProperty("keycloak.docker.image");

    private boolean useHttps;
    private Integer fixedPort;

    private static String getKeycloakImageName() {
        if (KEYCLOAK_DOCKER_IMAGE != null) {
            return KEYCLOAK_DOCKER_IMAGE;
        } else if (KEYCLOAK_VERSION != null) {
            return "quay.io/keycloak/keycloak:" + KEYCLOAK_VERSION;
        } else {
            throw new ConfigurationException("Please set either 'keycloak.docker.image' or 'keycloak.version' system property");
        }
    }

    public KeycloakContainer() {
        this(getKeycloakImageName());
    }

    public KeycloakContainer(String keycloakImageName) {
        super(keycloakImageName);
        withExposedPorts(8080, 8443);
        withEnv("DB_VENDOR", "H2");
        withEnv("KEYCLOAK_USER", "admin");
        withEnv("KEYCLOAK_PASSWORD", "admin");
        withEnv("KEYCLOAK_HTTPS_PORT", "8443");
        waitingFor(Wait.forHttp("/auth").forPort(8080));
        withNetwork(Network.SHARED);
        withNetworkAliases("keycloak");
    }

    public KeycloakContainer withUseHttps(boolean useHttps) {
        this.useHttps = useHttps;
        return this;
    }

    public KeycloakContainer withFixedPort(int fixedPort) {
        this.fixedPort = fixedPort;
        return this;
    }

    @Override
    protected void doStart() {
        if (fixedPort != null) {
            addFixedExposedPort(fixedPort, getPort());
        }
        if (useHttps) {
            RestAssured.useRelaxedHTTPSValidation();
        }
        super.doStart();
    }

    public int getPort() {
        return useHttps ? 8443 : 8080;
    }

    public String getServerUrl() {
        return String.format("%s://%s:%d/auth",
                useHttps ? "https" : "http", this.getHost(), this.getMappedPort(getPort()));
    }

    public void createRealmFromPath(String path) {
        RealmRepresentation representation = readRealmFile(path);
        postRealm(representation);
    }

    public RealmRepresentation readRealmFile(String realmPath) {
        try {
            return readRealmFile(Path.of(realmPath).toUri().toURL(), realmPath);
        } catch (MalformedURLException ex) {
            // Will not happen as this method is called only when it is confirmed the file exists
            throw new RuntimeException(ex);
        }
    }

    public RealmRepresentation readRealmFile(URL url, String realmPath) {
        try {
            try (InputStream is = url.openStream()) {
                return JsonSerialization.readValue(is, RealmRepresentation.class);
            }
        } catch (IOException ex) {
            LOG.errorf("Realm %s resource can not be opened: %s", realmPath, ex.getMessage());
        }
        return null;
    }

    public String getAdminAccessToken() {
        return RestAssured.given()
                .param("grant_type", "password")
                .param("username", "admin")
                .param("password", "admin")
                .param("client_id", "admin-cli")
                .when()
                .post(getServerUrl() + "/realms/master/protocol/openid-connect/token")
                .as(AccessTokenResponse.class).getToken();
    }

    public void postRealm(RealmRepresentation realm) {
        try {
            RestAssured.given().auth().oauth2(getAdminAccessToken())
                    .contentType("application/json")
                    .body(JsonSerialization.writeValueAsBytes(realm))
                    .when()
                    .post(getServerUrl() + "/admin/realms").then()
                    .statusCode(201);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteRealm(String realm) {
        RestAssured.given().auth().oauth2(getAdminAccessToken())
                .when()
                .delete(getServerUrl() + "/admin/realms/" + realm).then().statusCode(204);

    }

}
