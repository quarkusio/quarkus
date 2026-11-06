package org.jboss.resteasy.reactive.client.impl;

import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;

import jakarta.ws.rs.core.GenericType;

import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.stork.Stork;
import io.smallrye.stork.api.Service;
import io.smallrye.stork.api.ServiceInstance;

class StorkClientRequestFilterTest {
    @ParameterizedTest
    @ValueSource(strings = { "stork", "storks" })
    void testQueryEncoding(String scheme) {
        // Given
        Stork mockStork = mock(Stork.class);
        try (MockedStatic<Stork> storkStatic = mockStatic(Stork.class)) {
            storkStatic.when(Stork::getInstance).thenReturn(mockStork);
            Service mockedService = mock(Service.class);
            ServiceInstance mockedServiceInstance = mock(ServiceInstance.class);
            when(mockedServiceInstance.getPort()).thenReturn(8080);
            when(mockStork.getService(anyString())).thenReturn(mockedService);
            when(mockedService.selectInstanceAndRecordStart(anyBoolean()))
                    .thenReturn(Uni.createFrom().item(mockedServiceInstance));
            StorkClientRequestFilter filter = new StorkClientRequestFilter();
            ResteasyReactiveClientRequestContext context = mock(ResteasyReactiveClientRequestContext.class);

            URI originalUri = URI
                    .create(scheme + "://my-service/some/path?foo=bar%20baz&special=%26%3D&ae=%C3%A4&oe=%C3%B6&ue=%C3%BC");
            when(context.getUri()).thenReturn(originalUri);
            when(context.getResponseType()).thenReturn(new GenericType<>(Multi.class));

            // When
            filter.filter(context);

            // Then
            verify(context).setUri(argThat(uri -> {
                // Query should be preserved and properly encoded
                String resolvedScheme = "storks".equals(scheme) ? "https" : "http";
                return (resolvedScheme
                        + "://localhost:8080/some/path?foo=bar%20baz&special=%26%3D&ae=%C3%A4&oe=%C3%B6&ue=%C3%BC")
                        .equals(uri.toString());
            }));
        }
    }
}
