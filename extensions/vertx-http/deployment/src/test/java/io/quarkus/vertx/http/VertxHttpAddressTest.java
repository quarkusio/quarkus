package io.quarkus.vertx.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
            .withApplicationRoot((jar) -> jar.addClasses(StartupObserver.class, AddressServiceConsumer.class))
            .addBuildChainCustomizer(AddressServiceConsumer.buildStep());

    @Inject
    @Named("vertx-http")
    Address address;

    @Inject
    StartupObserver startupObserver;

    @Test
    void injectAddress() {
        assertThat(address.uri()).hasToString("http://localhost:8081");
        assertThat(address.domainSocket()).isEmpty();
    }

    @Test
    void addressAvailableOnStartup() {
        // the sockets are bound before the application starts, so a startup observer already sees the address
        assertThat(startupObserver.address()).isEqualTo(address);
    }

    @Test
    void requestAddressFromServiceGraph() {
        // a service that declares request(Address.class, "vertx-http") receives the address of the main listener,
        // and an empty optional for the management listener, which is not bound
        assertThat(AddressServiceConsumer.main).contains(address);
        assertThat(AddressServiceConsumer.management).isEmpty();
    }

    @Test
    void resolveAddress() {
        assertThat(Address.resolve("quarkus://vertx-http")).isEqualTo(address);
    }

    @Test
    void resolveWithPathAndQuery() {
        Address resolved = Address.resolve("quarkus://user@vertx-http/hello?x=1#top");
        assertThat(resolved.uri()).hasToString("http://user@localhost:8081/hello?x=1#top");
    }

    @Test
    void passThroughPlainUri() {
        assertThat(Address.resolve("https://example.com/api").uri()).hasToString("https://example.com/api");
    }

    @Test
    void managementNotListening() {
        assertThatThrownBy(() -> Address.resolve("quarkus://vertx-management"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("vertx-management");
    }

    @Test
    void unknownName() {
        // the registry cannot tell an unknown name from a listener that is not bound
        assertThatThrownBy(() -> Address.resolve("quarkus://nope"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nope");
    }

    @Test
    void referenceMustNotCarryPort() {
        assertThatThrownBy(() -> Address.resolve("quarkus://vertx-http:8080"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("port");
    }

    @ApplicationScoped
    public static class StartupObserver {
        private volatile Address address;

        void onStart(@Observes StartupEvent event, @Named("vertx-http") Address address) {
            this.address = address;
        }

        // read through the client proxy; a direct field read would hit the proxy, not the instance
        Address address() {
            return address;
        }
    }
}
