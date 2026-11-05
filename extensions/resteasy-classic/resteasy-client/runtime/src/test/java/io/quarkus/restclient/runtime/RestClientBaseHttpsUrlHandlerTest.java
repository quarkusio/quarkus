package io.quarkus.restclient.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.MalformedURLException;

import org.junit.jupiter.api.Test;

/**
 * Native executables built without the https URL protocol handler throw a {@link MalformedURLException}, whose message
 * GraalVM reworded in 25.1: both wordings must be recognized to report that native SSL support is disabled.
 * <p>
 * The messages are copied from {@code com.oracle.svm.core.jdk.JavaNetSubstitutions} in GraalVM. The native integration
 * tests of {@code integration-tests/main} check the actual message, but CI only builds them with GraalVM 25.0.x.
 */
class RestClientBaseHttpsUrlHandlerTest {

    // release/graal-vm/25.0 branch
    @Test
    void graalVM25_0Message() {
        assertThat(RestClientBase.isHttpsUrlHandlerDisabled(new MalformedURLException(
                "Accessing a URL protocol that was not enabled. The URL protocol https is supported but not enabled by default."
                        + " It must be enabled by adding the --enable-url-protocols=https option to the native-image command.")))
                .isTrue();
    }

    // release/graal-vm/25.1 branch, unchanged since
    @Test
    void graalVM25_1Message() {
        assertThat(RestClientBase.isHttpsUrlHandlerDisabled(new MalformedURLException(
                "Accessing a URL protocol that was not enabled. The URL protocol https is supported but not enabled by default."
                        + " It must be enabled by adding the sun.net.www.protocol.https.Handler to reachability metadata.")))
                .isTrue();
    }

    @Test
    void httpHandlerDisabled() {
        assertThat(RestClientBase.isHttpsUrlHandlerDisabled(new MalformedURLException(
                "Accessing a URL protocol that was not enabled. The URL protocol http is supported but not enabled by default."
                        + " It must be enabled by adding the sun.net.www.protocol.http.Handler to reachability metadata.")))
                .isFalse();
    }

    @Test
    void otherMalformedUrls() {
        assertThat(RestClientBase.isHttpsUrlHandlerDisabled(new MalformedURLException("no protocol: localhost:8080")))
                .isFalse();
        assertThat(RestClientBase.isHttpsUrlHandlerDisabled(new MalformedURLException())).isFalse();
    }
}
