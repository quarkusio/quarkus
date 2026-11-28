package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import jakarta.ws.rs.WebApplicationException;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.configuration.EchoResource;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * With {@code capture-stacktrace} enabled, the exception built from an error response carries the stack trace of the
 * REST Client caller, starting at the first frame outside RESTEasy Reactive and generated classes.
 */
public class CaptureStacktraceTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(EchoResource.class, HelloClient2.class))
            .overrideRuntimeConfigKey("quarkus.rest-client.hello2.url",
                    "http://localhost:${quarkus.http.test-port:8081}/wrong-url")
            .overrideRuntimeConfigKey("quarkus.rest-client.capture-stacktrace", "true");

    @RestClient
    HelloClient2 client;

    @Test
    void stackTraceStartsAtCaller() {
        ClientWebApplicationException e = catchThrowableOfType(ClientWebApplicationException.class,
                () -> callClient());

        assertThat(e).hasCauseInstanceOf(WebApplicationException.class);
        StackTraceElement[] callerStackTrace = e.getCause().getStackTrace();
        assertThat(callerStackTrace).isNotEmpty();
        assertThat(callerStackTrace[0].getClassName()).isEqualTo(CaptureStacktraceTest.class.getName());
        assertThat(callerStackTrace[0].getMethodName()).isEqualTo("callClient");
        assertThat(callerStackTrace[1].getClassName()).isEqualTo(CaptureStacktraceTest.class.getName());
    }

    private void callClient() {
        client.echo("Bob");
    }
}
