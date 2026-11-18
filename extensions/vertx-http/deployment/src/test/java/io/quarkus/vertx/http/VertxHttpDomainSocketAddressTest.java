package io.quarkus.vertx.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnixDomainSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.Address;
import io.quarkus.test.QuarkusExtensionTest;

@DisabledOnOs(OS.WINDOWS)
class VertxHttpDomainSocketAddressTest {
    private static final Path SOCKET = Path.of("/tmp", "quarkus-address-" + ProcessHandle.current().pid() + ".sock");

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .overrideRuntimeConfigKey("quarkus.http.domain-socket-enabled", "true")
            .overrideRuntimeConfigKey("quarkus.http.domain-socket", SOCKET.toString())
            .overrideRuntimeConfigKey("quarkus.vertx.native-transport", "disabled");

    @Inject
    @Named("vertx-http")
    Address address;

    @Inject
    HttpServer httpServer;

    @AfterAll
    static void deleteSocket() throws IOException {
        Files.deleteIfExists(SOCKET);
    }

    @Test
    void tcpAndDomainSocketAreOneAddress() {
        assertThat(address.protocol()).isEqualTo("http");
        // the domain socket sorts first, whatever order the server bound them in
        assertThat(address.addresses()).containsExactly(UnixDomainSocketAddress.of(SOCKET),
                new InetSocketAddress("localhost", 8081));
        assertThat(address.unix()).contains(UnixDomainSocketAddress.of(SOCKET));
        assertThat(address.inet()).contains(new InetSocketAddress("localhost", 8081));
        assertThat(httpServer.getAddress()).contains(address);
    }
}
