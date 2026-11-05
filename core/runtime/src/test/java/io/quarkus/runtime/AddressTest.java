package io.quarkus.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;

import org.junit.jupiter.api.Test;

/**
 * The parts of {@link Address} that do not need a running application: parsing a reference, passing a plain URI
 * through, and composing a reference onto a resolved base.
 */
class AddressTest {
    @Test
    void plainUriPassesThroughWithoutApplication() {
        // the REST clients resolve every base URL this way, also in unit tests where no application runs
        Address address = Address.resolve("http://localhost:8080/api");
        assertThat(address.uri()).hasToString("http://localhost:8080/api");
        assertThat(address.domainSocket()).isEmpty();
    }

    @Test
    void referenceNeedsRunningApplication() {
        assertThatThrownBy(() -> Address.resolve("quarkus://vertx-http"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no application is running");
    }

    @Test
    void blankReferenceIsRejected() {
        assertThatThrownBy(() -> Address.resolve(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void invalidUriIsRejected() {
        assertThatThrownBy(() -> Address.resolve("http://local host"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot resolve");
    }

    @Test
    void referenceComposesOntoBase() {
        Address base = Address.of(URI.create("http://localhost:8081/root/"));
        Address resolved = base.resolveReference(URI.create("quarkus://user@vertx-http/hello?x=1#top"));
        // the base scheme, host, port and path are kept; the user info, path, query and fragment come from the
        // reference, and the trailing slash of the base path does not double up
        assertThat(resolved.uri()).hasToString("http://user@localhost:8081/root/hello?x=1#top");
    }

    @Test
    void bareReferenceResolvesToBase() {
        Address base = Address.of(URI.create("https://localhost:8443"), "/tmp/quarkus.sock");
        Address resolved = base.resolveReference(URI.create("quarkus://vertx-http"));
        assertThat(resolved.uri()).hasToString("https://localhost:8443");
        assertThat(resolved.domainSocket()).contains("/tmp/quarkus.sock");
    }

    @Test
    void equalityIsByUriAndDomainSocket() {
        URI uri = URI.create("http://localhost:8080");
        assertThat(Address.of(uri)).isEqualTo(Address.of(uri)).hasToString("http://localhost:8080");
        assertThat(Address.of(uri)).isNotEqualTo(Address.of(uri, "/tmp/quarkus.sock"));
    }
}
