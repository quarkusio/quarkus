package io.quarkus.vertx.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.Address;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.QuarkusExtensionTest;

class VertxHttpAddressTest {
    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(StartupObserver.class, AddressServiceConsumer.class, ReceivedAddresses.class))
            .addBuildChainCustomizer(AddressServiceConsumer.buildStep());

    @Inject
    @Named("vertx-http")
    Address address;

    @Inject
    HttpServer httpServer;

    @Inject
    StartupObserver startupObserver;

    @Inject
    ReceivedAddresses received;

    @Test
    void injectAddress() {
        assertThat(address.protocol()).isEqualTo("http");
        assertThat(address.inet()).contains(new InetSocketAddress("localhost", 8081));
        assertThat(address.unix()).isEmpty();
    }

    @Test
    void addressFollowsTheHttpServer() {
        assertThat(httpServer.getAddress()).contains(address);
        assertThat(httpServer.getSecureAddress()).isEmpty();
        assertThat(httpServer.getManagementAddress()).isEmpty();
    }

    @Test
    void addressAvailableOnStartup() {
        assertThat(startupObserver.address()).isEqualTo(address);
    }

    @Test
    void requestAddressFromServiceGraph() {
        assertThat(received.main()).contains(address);
        assertThat(received.management()).isEmpty();
    }

    @ApplicationScoped
    public static class StartupObserver {
        private volatile Address address;

        void onStart(@Observes StartupEvent event, @Named("vertx-http") Address address) {
            this.address = address;
        }

        Address address() {
            return address;
        }
    }
}
