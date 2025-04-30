package io.quarkus.test.keycloak.server;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.restassured.RestAssured;

public class KeycloakContainer extends GenericContainer<KeycloakContainer> {

    private static final String KEYCLOAK_VERSION = System.getProperty("keycloak.version");
    private static final String KEYCLOAK_DOCKER_IMAGE = System.getProperty("keycloak.docker.image");

    private boolean useHttps;
    private Integer fixedPort;
    private final boolean legacy;

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
        this(DockerImageName.parse(getKeycloakImageName()));
    }

    public KeycloakContainer(DockerImageName keycloakImageName) {
        super(keycloakImageName);
        this.legacy = keycloakImageName.getVersionPart().endsWith("-legacy");
        withExposedPorts(8080, 8443);
        // Keycloak env vars
        withEnv("KC_HTTP_ENABLED", "true");
        withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin");
        withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin");
        withEnv("KC_HOSTNAME_STRICT", "false");
        withEnv("KC_HOSTNAME_STRICT_HTTPS", "false");
        withEnv("KC_STORAGE", "chm");
        // Keycloak legacy env vars
        withEnv("DB_VENDOR", "H2");
        withEnv("KEYCLOAK_USER", "admin");
        withEnv("KEYCLOAK_PASSWORD", "admin");
        withEnv("KEYCLOAK_HTTPS_PORT", "8443");
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
        if (!legacy && getCommandParts().length == 0) {
            withCommand("start-dev");
        }
        if (fixedPort != null) {
            addFixedExposedPort(fixedPort, getPort());
        }
        if (useHttps) {
            RestAssured.useRelaxedHTTPSValidation();
            waitingFor(Wait.forHttps(getAuthPath()).forPort(8443).allowInsecure());
        } else {
            waitingFor(Wait.forHttp(getAuthPath()).forPort(8080));
        }
        super.doStart();
    }

    public int getPort() {
        return useHttps ? 8443 : 8080;
    }

    private String getAuthPath() {
        return legacy ? "/auth" : "";
    }

    public String getServerUrl() {
        return String.format("%s://%s:%d" + getAuthPath(),
                useHttps ? "https" : "http", this.getHost(), this.getMappedPort(getPort()));
    }

    public String getInternalUrl() {
        return String.format("%s://keycloak:%d" + getAuthPath(),
                useHttps ? "https" : "http", getPort());
    }
}
