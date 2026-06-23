package io.quarkus.vertx.http.runtime;

import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

@ExtendWith(MockitoExtension.class)
class HostValidationFilterTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    RoutingContext context;

    @Test
    void requestWithoutAuthority() {
        when(context.request().authority()).thenReturn(null);
        HttpServerResponse response = context.response();

        new HostValidationFilter(Set.of("localhost")).handle(context);

        Mockito.verify(response).setStatusCode(403);
        Mockito.verify(response).end();
    }

}
