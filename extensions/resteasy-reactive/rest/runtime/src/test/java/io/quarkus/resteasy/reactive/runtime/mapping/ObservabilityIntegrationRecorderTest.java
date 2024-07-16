package io.quarkus.resteasy.reactive.runtime.mapping;

import io.quarkus.resteasy.reactive.server.runtime.observability.ObservabilityIntegrationRecorder;
import io.vertx.ext.web.RoutingContext;
import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.handlers.RestInitialHandler;
import org.jboss.resteasy.reactive.server.mapping.RequestMapper;
import org.jboss.resteasy.reactive.server.mapping.URITemplate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ObservabilityIntegrationRecorderTest {

    private static RoutingContext rc;
    private static Deployment deployment;

    @BeforeAll
    public static void setup() {
        rc = mock(RoutingContext.class);
        deployment = mock(Deployment.class);
        when(rc.normalizedPath()).thenReturn("/foo");
        when(deployment.getClassMappers()).thenReturn(getRequestPaths());
    }

    @Test
    public void testSetTemplatePathWithoutPathPrefixDoesNotThrow() {
        when(deployment.getPrefix()).thenReturn("");
        assertDoesNotThrow(() -> ObservabilityIntegrationRecorder.setTemplatePath(rc, deployment), "Should not throw exception when prefix is not set");
    }

    @Test
    public void testSetTemplatePathWithPathPrefixDoesNotThrow() {
        when(deployment.getPrefix()).thenReturn("/foo");
        assertDoesNotThrow(() -> ObservabilityIntegrationRecorder.setTemplatePath(rc, deployment), "Should not throw exception when prefix is set");
    }

    private static ArrayList<RequestMapper.RequestPath<RestInitialHandler.InitialMatch>> getRequestPaths() {
        RestInitialHandler.InitialMatch initialMatch = mock(RestInitialHandler.InitialMatch.class);
        ArrayList<RequestMapper.RequestPath<RestInitialHandler.InitialMatch>> classMappers = new ArrayList<>();
        classMappers.add(new RequestMapper.RequestPath<>(true, new URITemplate("/", true), initialMatch));
        classMappers.add(new RequestMapper.RequestPath<>(true, new URITemplate("{param}/bar", true), initialMatch));
        return classMappers;
    }
}
