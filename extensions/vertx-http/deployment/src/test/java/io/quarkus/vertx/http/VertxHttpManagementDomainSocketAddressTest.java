package io.quarkus.vertx.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnixDomainSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.Address;
import io.quarkus.test.QuarkusExtensionTest;

@DisabledOnOs(OS.WINDOWS)
class VertxHttpManagementDomainSocketAddressTest {
    private static final Path SOCKET = Path.of("/tmp", "quarkus-mgmt-" + ProcessHandle.current().pid() + ".sock");

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ManagementRoute.class)
                    .addAsResource(new StringAsset("quarkus.management.enabled=true\n"), "application.properties"))
            .addBuildChainCustomizer(ManagementRoute.buildStep())
            .overrideRuntimeConfigKey("quarkus.management.domain-socket-enabled", "true")
            .overrideRuntimeConfigKey("quarkus.management.domain-socket", SOCKET.toString())
            .overrideRuntimeConfigKey("quarkus.vertx.native-transport", "disabled");

    @Inject
    @Named("vertx-management")
    Address management;

    @Inject
    HttpServer httpServer;

    @AfterAll
    static void deleteSocket() throws IOException {
        Files.deleteIfExists(SOCKET);
    }

    @Test
    void tcpAndDomainSocketAreOneAddress() {
        assertThat(management.protocol()).isEqualTo("http");
        assertThat(management.addresses()).containsExactly(UnixDomainSocketAddress.of(SOCKET),
                new InetSocketAddress("localhost", 9001));
        assertThat(httpServer.getManagementAddress()).contains(management);
    }
}
