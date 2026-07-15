package io.quarkus.vertx.http.runtime;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.vertx.core.net.HostAndPort;
import io.vertx.ext.web.RoutingContext;

@ExtendWith(MockitoExtension.class)
class HostValidationFilterTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    RoutingContext context;

    @Test
    void requestWithoutAuthority() {
        when(context.request().authority()).thenReturn(null);

        new HostValidationFilter(Set.of("localhost")).handle(context);

        verify(context.response()).setStatusCode(400);
        verify(context.response()).end();
        verify(context, never()).next();
    }

    @Test
    void allowedHostMatch() {
        when(context.request().authority()).thenReturn(HostAndPort.create("localhost", 8080));

        new HostValidationFilter(Set.of("localhost")).handle(context);

        verify(context).next();
        verify(context.response(), never()).setStatusCode(400);
    }

    @Test
    void hostNotInAllowedSet() {
        when(context.request().authority()).thenReturn(HostAndPort.create("evil.com", 8080));

        new HostValidationFilter(Set.of("localhost")).handle(context);

        verify(context.response()).setStatusCode(400);
        verify(context.response()).end();
        verify(context, never()).next();
    }

    @Test
    void caseInsensitiveMatching() {
        when(context.request().authority()).thenReturn(HostAndPort.create("LocalHost", 8080));

        new HostValidationFilter(Set.of("localhost")).handle(context);

        verify(context).next();
        verify(context.response(), never()).setStatusCode(400);
    }

    @Test
    void hostWithPort() {
        when(context.request().authority()).thenReturn(HostAndPort.create("localhost", 9999));

        new HostValidationFilter(Set.of("localhost")).handle(context);

        verify(context).next();
        verify(context.response(), never()).setStatusCode(400);
    }

    @Test
    void multipleAllowedHosts() {
        when(context.request().authority()).thenReturn(HostAndPort.create("api.example.com", 443));

        new HostValidationFilter(Set.of("localhost", "api.example.com")).handle(context);

        verify(context).next();
        verify(context.response(), never()).setStatusCode(400);
    }
}
