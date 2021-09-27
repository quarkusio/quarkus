package io.quarkus.vertx.http.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.ParsableMIMEValue;

@ExtendWith(MockitoExtension.class)
class QuarkusErrorHandlerTest {

    private static final Throwable testError = new IllegalStateException("test123");

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    RoutingContext routingContext;

    @Test
    public void string_with_tab_should_be_correctly_escaped() {
        String initial = "String with a tab\tcharacter";
        String json = QuarkusErrorHandler.escapeJsonString(initial);
        String parsed = Json.decodeValue('"' + json + '"', String.class);
        assertEquals(initial, parsed);
    }

    @Test
    public void string_with_backslash_should_be_correctly_escaped() {
        String initial = "String with a backslash \\ character";
        String json = QuarkusErrorHandler.escapeJsonString(initial);
        String parsed = Json.decodeValue('"' + json + '"', String.class);
        assertEquals(initial, parsed);
    }

    @Test
    public void string_with_quotes_should_be_correctly_escaped() {
        String initial = "String with \"quoted text\"";
        String json = QuarkusErrorHandler.escapeJsonString(initial);
        String parsed = Json.decodeValue('"' + json + '"', String.class);
        assertEquals(initial, parsed);
    }

    @Test
    public void json_content_type_hint_should_be_respected_if_not_accepted() {
        QuarkusErrorHandler errorHandler = new QuarkusErrorHandler(false, Optional.of(HttpConfiguration.PayloadHint.JSON));
        Mockito.when(routingContext.failure()).thenReturn(testError);
        Mockito.when(routingContext.parsedHeaders().findBestUserAcceptedIn(any(), any()))
                .thenReturn(new ParsableMIMEValue("application/foo+json").forceParse());
        errorHandler.handle(routingContext);
        Mockito.verify(routingContext.response().headers()).set(HttpHeaderNames.CONTENT_TYPE,
                "application/json; charset=utf-8");
    }

    @Test
    public void json_content_type_hint_should_be_ignored_if_accepted() {
        QuarkusErrorHandler errorHandler = new QuarkusErrorHandler(false, Optional.of(HttpConfiguration.PayloadHint.JSON));
        Mockito.when(routingContext.failure()).thenReturn(testError);
        Mockito.when(routingContext.parsedHeaders().findBestUserAcceptedIn(any(), any()))
                .thenReturn(new ParsableMIMEValue("text/html").forceParse());
        errorHandler.handle(routingContext);
        Mockito.verify(routingContext.response().headers()).set(HttpHeaderNames.CONTENT_TYPE,
                "text/html; charset=utf-8");
    }

    @Test
    public void content_type_should_default_to_json_if_accepted() {
        QuarkusErrorHandler errorHandler = new QuarkusErrorHandler(false, Optional.empty());
        Mockito.when(routingContext.failure()).thenReturn(testError);
        Mockito.when(routingContext.parsedHeaders().findBestUserAcceptedIn(any(), any()))
                .thenReturn(new ParsableMIMEValue("application/json").forceParse());
        errorHandler.handle(routingContext);
        Mockito.verify(routingContext.response().headers()).set(HttpHeaderNames.CONTENT_TYPE,
                "application/json; charset=utf-8");
    }

    @Test
    public void html_content_type_hint_should_be_respected_if_not_accepted() {
        QuarkusErrorHandler errorHandler = new QuarkusErrorHandler(false, Optional.of(HttpConfiguration.PayloadHint.HTML));
        Mockito.when(routingContext.failure()).thenReturn(testError);
        Mockito.when(routingContext.parsedHeaders().findBestUserAcceptedIn(any(), any()))
                .thenReturn(new ParsableMIMEValue("application/foo+json").forceParse());
        errorHandler.handle(routingContext);
        Mockito.verify(routingContext.response().headers()).set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=utf-8");
    }

    @Test
    public void html_content_type_hint_should_be_ignored_if_accepted() {
        QuarkusErrorHandler errorHandler = new QuarkusErrorHandler(false, Optional.of(HttpConfiguration.PayloadHint.HTML));
        Mockito.when(routingContext.failure()).thenReturn(testError);
        Mockito.when(routingContext.parsedHeaders().findBestUserAcceptedIn(any(), any()))
                .thenReturn(new ParsableMIMEValue("application/json").forceParse());
        errorHandler.handle(routingContext);
        Mockito.verify(routingContext.response().headers()).set(HttpHeaderNames.CONTENT_TYPE,
                "application/json; charset=utf-8");
    }

    @Test
    public void content_type_should_default_to_html_if_accepted() {
        QuarkusErrorHandler errorHandler = new QuarkusErrorHandler(false, Optional.empty());
        Mockito.when(routingContext.failure()).thenReturn(testError);
        Mockito.when(routingContext.parsedHeaders().findBestUserAcceptedIn(any(), any()))
                .thenReturn(new ParsableMIMEValue("text/html").forceParse());
        errorHandler.handle(routingContext);
        Mockito.verify(routingContext.response().headers()).set(HttpHeaderNames.CONTENT_TYPE,
                "text/html; charset=utf-8");
    }
}
