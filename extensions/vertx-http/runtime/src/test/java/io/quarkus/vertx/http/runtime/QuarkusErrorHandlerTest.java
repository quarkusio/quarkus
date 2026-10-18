package io.quarkus.vertx.http.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.security.AuthenticationCompletionException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.vertx.http.runtime.VertxHttpConfig.PayloadHint;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticator;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.ParsableMIMEValue;

@ExtendWith(MockitoExtension.class)
class QuarkusErrorHandlerTest {

    private static final Throwable testError = new IllegalStateException("test123");

    private final QuarkusErrorHandler errorHandler = new QuarkusErrorHandler(false, false, Optional.empty());

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
        QuarkusErrorHandler errorHandler = new QuarkusErrorHandler(false, false,
                Optional.of(PayloadHint.JSON));
        Mockito.when(routingContext.failure()).thenReturn(testError);
        Mockito.when(routingContext.parsedHeaders().findBestUserAcceptedIn(any(), any()))
                .thenReturn(new ParsableMIMEValue("application/foo+json").forceParse());
        errorHandler.handle(routingContext);
        Mockito.verify(routingContext.response().headers()).set(HttpHeaderNames.CONTENT_TYPE,
                "application/json; charset=utf-8");
    }

    @Test
    public void json_content_type_hint_should_be_ignored_if_accepted() {
        QuarkusErrorHandler errorHandler = new QuarkusErrorHandler(false, false,
                Optional.of(PayloadHint.JSON));
        Mockito.when(routingContext.failure()).thenReturn(testError);
        Mockito.when(routingContext.parsedHeaders().findBestUserAcceptedIn(any(), any()))
                .thenReturn(new ParsableMIMEValue("text/html").forceParse());
        errorHandler.handle(routingContext);
        Mockito.verify(routingContext.response().headers()).set(HttpHeaderNames.CONTENT_TYPE,
                "text/html; charset=utf-8");
    }

    @Test
    public void content_type_should_default_to_json_if_accepted() {
        Mockito.when(routingContext.failure()).thenReturn(testError);
        Mockito.when(routingContext.parsedHeaders().findBestUserAcceptedIn(any(), any()))
                .thenReturn(new ParsableMIMEValue("application/json").forceParse());
        errorHandler.handle(routingContext);
        Mockito.verify(routingContext.response().headers()).set(HttpHeaderNames.CONTENT_TYPE,
                "application/json; charset=utf-8");
    }

    @Test
    public void html_content_type_hint_should_be_respected_if_not_accepted() {
        QuarkusErrorHandler errorHandler = new QuarkusErrorHandler(false, false,
                Optional.of(PayloadHint.HTML));
        Mockito.when(routingContext.failure()).thenReturn(testError);
        Mockito.when(routingContext.parsedHeaders().findBestUserAcceptedIn(any(), any()))
                .thenReturn(new ParsableMIMEValue("application/foo+json").forceParse());
        errorHandler.handle(routingContext);
        Mockito.verify(routingContext.response().headers()).set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=utf-8");
    }

    @Test
    public void html_content_type_hint_should_be_ignored_if_accepted() {
        QuarkusErrorHandler errorHandler = new QuarkusErrorHandler(false, false,
                Optional.of(PayloadHint.HTML));
        Mockito.when(routingContext.failure()).thenReturn(testError);
        Mockito.when(routingContext.parsedHeaders().findBestUserAcceptedIn(any(), any()))
                .thenReturn(new ParsableMIMEValue("application/json").forceParse());
        errorHandler.handle(routingContext);
        Mockito.verify(routingContext.response().headers()).set(HttpHeaderNames.CONTENT_TYPE,
                "application/json; charset=utf-8");
    }

    @Test
    public void content_type_should_default_to_html_if_accepted() {
        Mockito.when(routingContext.failure()).thenReturn(testError);
        Mockito.when(routingContext.parsedHeaders().findBestUserAcceptedIn(any(), any()))
                .thenReturn(new ParsableMIMEValue("text/html").forceParse());
        errorHandler.handle(routingContext);
        Mockito.verify(routingContext.response().headers()).set(HttpHeaderNames.CONTENT_TYPE,
                "text/html; charset=utf-8");
    }

    @Test
    public void null_exception_sets_status_and_ends() {
        Mockito.when(routingContext.failure()).thenReturn(null);
        Mockito.when(routingContext.statusCode()).thenReturn(404);
        HttpServerResponse response = routingContext.response();

        errorHandler.handle(routingContext);

        Mockito.verify(response).setStatusCode(404);
        Mockito.verify(response).end();
    }

    @Test
    public void unauthorized_exception_without_authenticator_returns_401() {
        Mockito.when(routingContext.failure()).thenReturn(new UnauthorizedException());
        Mockito.when(routingContext.<HttpAuthenticator> get(HttpAuthenticator.class.getName()))
                .thenReturn(null);
        HttpServerResponse response = routingContext.response();

        errorHandler.handle(routingContext);

        Mockito.verify(response).setStatusCode(HttpResponseStatus.UNAUTHORIZED.code());
    }

    @Test
    public void forbidden_exception_returns_403() {
        Mockito.when(routingContext.failure()).thenReturn(new ForbiddenException());
        HttpServerResponse response = routingContext.response();

        errorHandler.handle(routingContext);

        Mockito.verify(response).setStatusCode(HttpResponseStatus.FORBIDDEN.code());
    }

    @Test
    public void authentication_exception_sets_401_when_status_200() {
        Mockito.when(routingContext.failure()).thenReturn(new AuthenticationCompletionException());
        HttpServerResponse response = routingContext.response();
        Mockito.when(response.getStatusCode()).thenReturn(HttpResponseStatus.OK.code());

        errorHandler.handle(routingContext);

        Mockito.verify(response).setStatusCode(HttpResponseStatus.UNAUTHORIZED.code());
        Mockito.verify(response).end();
    }

    @Test
    public void rejected_execution_exception_returns_503() {
        Mockito.when(routingContext.failure()).thenReturn(new RejectedExecutionException());
        HttpServerResponse response = routingContext.response();

        errorHandler.handle(routingContext);

        Mockito.verify(response).setStatusCode(HttpResponseStatus.SERVICE_UNAVAILABLE.code());
    }

    @Test
    public void default_response_uses_json_for_normal_user_agent() {
        Mockito.when(routingContext.failure()).thenReturn(testError);
        Mockito.when(routingContext.parsedHeaders().findBestUserAcceptedIn(any(), any()))
                .thenReturn(null);
        Mockito.when(routingContext.request().getHeader("User-Agent")).thenReturn("Mozilla/5.0");

        errorHandler.handle(routingContext);

        Mockito.verify(routingContext.response().headers()).set(HttpHeaderNames.CONTENT_TYPE,
                "application/json; charset=utf-8");
    }

    @Test
    public void default_response_uses_text_for_curl() {
        Mockito.when(routingContext.failure()).thenReturn(testError);
        Mockito.when(routingContext.parsedHeaders().findBestUserAcceptedIn(any(), any()))
                .thenReturn(null);
        Mockito.when(routingContext.request().getHeader("User-Agent")).thenReturn("curl/7.68.0");

        errorHandler.handle(routingContext);

        Mockito.verify(routingContext.response().headers()).set(HttpHeaderNames.CONTENT_TYPE,
                "text/plain; charset=utf-8");
    }

    @Test
    public void default_response_uses_text_for_wget() {
        Mockito.when(routingContext.failure()).thenReturn(testError);
        Mockito.when(routingContext.parsedHeaders().findBestUserAcceptedIn(any(), any()))
                .thenReturn(null);
        Mockito.when(routingContext.request().getHeader("User-Agent")).thenReturn("Wget/1.21");

        errorHandler.handle(routingContext);

        Mockito.verify(routingContext.response().headers()).set(HttpHeaderNames.CONTENT_TYPE,
                "text/plain; charset=utf-8");
    }

    @Test
    public void response_already_ended_does_nothing() {
        Mockito.when(routingContext.failure()).thenReturn(testError);
        Mockito.when(routingContext.parsedHeaders().findBestUserAcceptedIn(any(), any()))
                .thenReturn(new ParsableMIMEValue("application/json").forceParse());
        Mockito.when(routingContext.response().ended()).thenReturn(true);

        errorHandler.handle(routingContext);

        Mockito.verify(routingContext.response(), Mockito.never()).end(any(String.class));
    }
}
