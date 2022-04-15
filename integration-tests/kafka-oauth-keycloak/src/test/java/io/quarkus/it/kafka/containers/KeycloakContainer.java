package io.quarkus.it.kafka.containers;

import java.io.FileWriter;

import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

public class KeycloakContainer extends FixedHostPortGenericContainer<KeycloakContainer> {

    public KeycloakContainer() {
        super("quay.io/keycloak/keycloak:16.1.1");
        withExposedPorts(8443);
        withFixedExposedPort(8080, 8080);
        withEnv("KEYCLOAK_USER", "admin");
        withEnv("KEYCLOAK_PASSWORD", "admin");
        withEnv("KEYCLOAK_HTTPS_PORT", "8443");
        withEnv("PROXY_ADDRESS_FORWARDING", "true");
        withEnv("KEYCLOAK_IMPORT", "/opt/jboss/keycloak/realms/kafka-authz-realm.json");
        waitingFor(Wait.forLogMessage(".*WFLYSRV0025.*", 1));
        withNetwork(Network.SHARED);
        withNetworkAliases("keycloak");
        withCopyFileToContainer(MountableFile.forClasspathResource("keycloak/realms/kafka-authz-realm.json"),
                "/opt/jboss/keycloak/realms/kafka-authz-realm.json");
        withCommand("-Dkeycloak.profile.feature.upload_scripts=enabled", "-b", "0.0.0.0");
    }

    public void createHostsFile() {
        try (FileWriter fileWriter = new FileWriter("target/hosts")) {
            String dockerHost = this.getHost();
            if ("localhost".equals(dockerHost)) {
                fileWriter.write("127.0.0.1 keycloak");
            } else {
                fileWriter.write(dockerHost + " keycloak");
            }
            fileWriter.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
