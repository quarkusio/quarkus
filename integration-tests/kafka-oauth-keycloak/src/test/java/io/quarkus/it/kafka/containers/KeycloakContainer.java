package io.quarkus.it.kafka.containers;

import java.io.FileWriter;

import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import com.github.dockerjava.api.command.InspectContainerResponse;

public class KeycloakContainer extends FixedHostPortGenericContainer<KeycloakContainer> {

    public KeycloakContainer() {
        super("quay.io/keycloak/keycloak:15.0.2");
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
        withCreateContainerCmdModifier(cmd -> {
            cmd.withEntrypoint("");
            cmd.withCmd("/bin/bash", "-c", "cd /opt/jboss/keycloak " +
                    "&& bin/jboss-cli.sh --file=ssl/keycloak-ssl.cli " +
                    "&& rm -rf standalone/configuration/standalone_xml_history/current " +
                    "&& cd .. " +
                    "&& /opt/jboss/tools/docker-entrypoint.sh -Dkeycloak.profile.feature.upload_scripts=enabled -b 0.0.0.0");
        });
    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo, boolean reused) {
        super.containerIsStarting(containerInfo);
        copyFileToContainer(MountableFile.forClasspathResource("certificates/ca-truststore.p12"),
                "/opt/jboss/keycloak/standalone/configuration/certs/ca-truststore.p12");
        copyFileToContainer(MountableFile.forClasspathResource("certificates/keycloak.server.keystore.p12"),
                "/opt/jboss/keycloak/standalone/configuration/certs/keycloak.server.keystore.p12");
        copyFileToContainer(MountableFile.forClasspathResource("keycloak/scripts/keycloak-ssl.cli"),
                "/opt/jboss/keycloak/ssl/keycloak-ssl.cli");
        copyFileToContainer(MountableFile.forClasspathResource("keycloak/realms/kafka-authz-realm.json"),
                "/opt/jboss/keycloak/realms/kafka-authz-realm.json");
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
