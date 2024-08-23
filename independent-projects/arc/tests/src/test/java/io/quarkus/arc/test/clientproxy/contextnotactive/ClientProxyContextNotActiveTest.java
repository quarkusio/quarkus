package io.quarkus.arc.test.clientproxy.contextnotactive;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.RequestScoped;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class ClientProxyContextNotActiveTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(RequestFoo.class);

    @Test
    public void testToStringIsDelegated() {
        RequestFoo foo = Arc.container().instance(RequestFoo.class).get();
        assertThatExceptionOfType(ContextNotActiveException.class).isThrownBy(() -> foo.ping())
                .withMessageContaining(
                        "RequestScoped context was not active when trying to obtain a bean instance for a client proxy of CLASS bean [class=io.quarkus.arc.test.clientproxy.contextnotactive.ClientProxyContextNotActiveTest$RequestFoo")
                .withMessageContaining(
                        "you can activate the request context for a specific method using the @ActivateRequestContext interceptor binding");
    }

    @RequestScoped
    static class RequestFoo {

        void ping() {
        }

    }
}
