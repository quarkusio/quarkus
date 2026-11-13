package io.quarkus.vertx.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.Address;
import io.quarkus.test.QuarkusExtensionTest;

class VertxHttpManagementAddressTest {
    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ManagementRoute.class, AddressServiceConsumer.class, ReceivedAddresses.class)
                    .addAsResource(new StringAsset("quarkus.management.enabled=true\n"), "application.properties"))
            .addBuildChainCustomizer(ManagementRoute.buildStep())
            .addBuildChainCustomizer(AddressServiceConsumer.buildStep());

    @Inject
    @Named("vertx-http")
    Address main;

    @Inject
    @Named("vertx-management")
    Address management;

    @Inject
    HttpServer httpServer;

    @Inject
    ReceivedAddresses received;

    @Test
    void injectMainAndManagement() {
        assertThat(main.protocol()).isEqualTo("http");
        assertThat(main.inet()).contains(new InetSocketAddress("localhost", 8081));
        assertThat(management.protocol()).isEqualTo("http");
        assertThat(management.inet()).contains(new InetSocketAddress("localhost", 9001));
        assertThat(httpServer.getManagementBaseUri()).map(Object::toString).contains("http://localhost:9001/q");
    }

    @Test
    void requestAddressesFromServiceGraph() {
        assertThat(received.main()).contains(main);
        assertThat(received.management()).contains(management);
    }
}
