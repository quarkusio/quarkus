package io.quarkus.vertx.http.runtime;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;

@ExtendWith(MockitoExtension.class)
class HttpCompressionHandlerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    RoutingContext context;

    private static final Set<String> COMPRESSED_MEDIA_TYPES = Set.of("application/json", "text/html");

    @Test
    void compressIfNeeded_no_content_encoding_header() {
        when(context.response().headers().get(HttpHeaders.CONTENT_ENCODING)).thenReturn(null);

        HttpCompressionHandler.compressIfNeeded(context, COMPRESSED_MEDIA_TYPES);

        verify(context.response().headers(), never()).remove(HttpHeaders.CONTENT_ENCODING);
    }

    @Test
    void compressIfNeeded_content_encoding_not_identity() {
        when(context.response().headers().get(HttpHeaders.CONTENT_ENCODING)).thenReturn("gzip");

        HttpCompressionHandler.compressIfNeeded(context, COMPRESSED_MEDIA_TYPES);

        verify(context.response().headers(), never()).remove(HttpHeaders.CONTENT_ENCODING);
    }

    @Test
    void compressIfNeeded_identity_but_no_content_type() {
        when(context.response().headers().get(HttpHeaders.CONTENT_ENCODING)).thenReturn("identity");
        when(context.response().headers().get(HttpHeaders.CONTENT_TYPE)).thenReturn(null);

        HttpCompressionHandler.compressIfNeeded(context, COMPRESSED_MEDIA_TYPES);

        verify(context.response().headers(), never()).remove(HttpHeaders.CONTENT_ENCODING);
    }

    @Test
    void compressIfNeeded_identity_and_matching_content_type() {
        when(context.response().headers().get(HttpHeaders.CONTENT_ENCODING)).thenReturn("identity");
        when(context.response().headers().get(HttpHeaders.CONTENT_TYPE)).thenReturn("application/json");

        HttpCompressionHandler.compressIfNeeded(context, COMPRESSED_MEDIA_TYPES);

        verify(context.response().headers()).remove(HttpHeaders.CONTENT_ENCODING);
    }

    @Test
    void compressIfNeeded_identity_and_non_matching_content_type() {
        when(context.response().headers().get(HttpHeaders.CONTENT_ENCODING)).thenReturn("identity");
        when(context.response().headers().get(HttpHeaders.CONTENT_TYPE)).thenReturn("text/plain");

        HttpCompressionHandler.compressIfNeeded(context, COMPRESSED_MEDIA_TYPES);

        verify(context.response().headers(), never()).remove(HttpHeaders.CONTENT_ENCODING);
    }

    @Test
    void compressIfNeeded_identity_and_content_type_with_params_matching() {
        when(context.response().headers().get(HttpHeaders.CONTENT_ENCODING)).thenReturn("identity");
        when(context.response().headers().get(HttpHeaders.CONTENT_TYPE)).thenReturn("application/json; charset=utf-8");

        HttpCompressionHandler.compressIfNeeded(context, COMPRESSED_MEDIA_TYPES);

        verify(context.response().headers()).remove(HttpHeaders.CONTENT_ENCODING);
    }

    @Test
    void compressIfNeeded_identity_and_content_type_with_params_not_matching() {
        when(context.response().headers().get(HttpHeaders.CONTENT_ENCODING)).thenReturn("identity");
        when(context.response().headers().get(HttpHeaders.CONTENT_TYPE)).thenReturn("text/plain; charset=utf-8");

        HttpCompressionHandler.compressIfNeeded(context, COMPRESSED_MEDIA_TYPES);

        verify(context.response().headers(), never()).remove(HttpHeaders.CONTENT_ENCODING);
    }

    @Test
    @SuppressWarnings("unchecked")
    void handle_registers_headers_end_handler() {
        Handler<RoutingContext> routeHandler = Mockito.mock(Handler.class);
        HttpCompressionHandler handler = new HttpCompressionHandler(routeHandler, COMPRESSED_MEDIA_TYPES);

        handler.handle(context);

        ArgumentCaptor<Handler<Void>> captor = ArgumentCaptor.forClass(Handler.class);
        verify(context).addHeadersEndHandler(captor.capture());
        Assertions.assertNotNull(captor.getValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    void handle_delegates_to_route_handler() {
        Handler<RoutingContext> routeHandler = Mockito.mock(Handler.class);
        HttpCompressionHandler handler = new HttpCompressionHandler(routeHandler, COMPRESSED_MEDIA_TYPES);

        handler.handle(context);

        verify(routeHandler).handle(context);
    }
}
