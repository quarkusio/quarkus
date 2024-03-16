package io.quarkus.vertx.http.runtime.attribute;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.netty.handler.codec.http.QueryStringDecoder;
import io.quarkus.vertx.http.runtime.filters.QuarkusRequestWrapper;
import io.quarkus.vertx.http.runtime.filters.accesslog.AccessLogHandler;
import io.quarkus.vertx.http.runtime.filters.accesslog.AccessLogReceiver;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.HostAndPort;
import io.vertx.core.net.impl.HostAndPortImpl;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.impl.RoutingContextImpl;

public class ConsolidateReroutedRequestsTest {

    @Test
    public void testDisabledNoReroutesRequestLineOnly() {
        test(
                false,
                Arrays.asList("%r", "%{REQUEST_LINE}"),
                Optional.empty(),
                HttpMethod.GET,
                HttpVersion.HTTP_1_1,
                "https",
                new HostAndPortImpl("example.org", 443),
                "/path1",
                "q1=v1",
                Collections.emptyList(),
                Arrays.asList(
                        "GET /path1?q1=v1 HTTP/1.1"));
    }

    @Test
    public void testDisabled2ReroutesRequestLineOnly() {
        test(
                false,
                Arrays.asList("%r", "%{REQUEST_LINE}"),
                Optional.empty(),
                HttpMethod.GET,
                HttpVersion.HTTP_1_1,
                "https",
                new HostAndPortImpl("example.org", 443),
                "/path1",
                "q1=v1",
                Arrays.asList(
                        new Reroute(null, "/path2", "q2=v2"),
                        new Reroute(null, "/path3", "q3=v3")),
                Arrays.asList(
                        "GET /path3?q3=v3 HTTP/1.1",
                        "GET /path3?q3=v3 HTTP/1.1",
                        "GET /path3?q3=v3 HTTP/1.1"));
    }

    @Test
    public void testDisabled1RerouteRequestPathOnly() {
        test(
                false,
                Arrays.asList("%R", "%{REQUEST_PATH}"),
                Optional.empty(),
                HttpMethod.GET,
                HttpVersion.HTTP_1_1,
                "https",
                new HostAndPortImpl("example.org", 443),
                "/path1",
                "q1=v1",
                Arrays.asList(
                        new Reroute(null, "/path2", "q2=v2")),
                Arrays.asList(
                        "/path2",
                        "/path2"));
    }

    @Test
    public void testDisabled2ReroutesOriginalRequestLineOnly() {
        test(
                false,
                Arrays.asList("%<r", "%{<REQUEST_LINE}"),
                Optional.empty(),
                HttpMethod.GET,
                HttpVersion.HTTP_1_1,
                "https",
                new HostAndPortImpl("example.org", 443),
                "/path1",
                "q1=v1",
                Arrays.asList(
                        new Reroute(null, "/path2", "q2=v2"),
                        new Reroute(null, "/path3", "q3=v3")),
                Arrays.asList(
                        "-",
                        "-",
                        "-"));
    }

    @Test
    public void testDisabled2ReroutesAllOriginalAndFinal() {
        test(
                false,
                Arrays.asList(
                        "%<r %r %<R %R %<q %q %{<BARE_QUERY_STRING} %{BARE_QUERY_STRING} %{<q,q} %{q,q} %<U %U",
                        "%{<REQUEST_LINE} %{REQUEST_LINE} %{<REQUEST_PATH} %{REQUEST_PATH} %{<QUERY_STRING} %{QUERY_STRING} %{<BARE_QUERY_STRING} %{BARE_QUERY_STRING} %{<q,q} %{q,q} %{<REQUEST_URL} %{REQUEST_URL}"),
                Optional.empty(),
                HttpMethod.GET,
                HttpVersion.HTTP_1_1,
                "https",
                new HostAndPortImpl("example.org", 443),
                "/path1",
                "q=v1",
                Arrays.asList(
                        new Reroute(null, "/path2", "q=v2"),
                        new Reroute(null, "/path3", "q=v3")),
                Arrays.asList(
                        "- GET /path3?q=v3 HTTP/1.1 - /path3 - ?q=v3 - q=v3 - v3 - /path3?q=v3",
                        "- GET /path3?q=v3 HTTP/1.1 - /path3 - ?q=v3 - q=v3 - v3 - /path3?q=v3",
                        "- GET /path3?q=v3 HTTP/1.1 - /path3 - ?q=v3 - q=v3 - v3 - /path3?q=v3"));
    }

    @Test
    public void testEnabledNoReroutesRequestLineOnly() {
        test(
                true,
                Arrays.asList("%r", "%{REQUEST_LINE}"),
                Optional.empty(),
                HttpMethod.GET,
                HttpVersion.HTTP_1_1,
                "https",
                new HostAndPortImpl("example.org", 443),
                "/path1",
                "q1=v1",
                Collections.emptyList(),
                Arrays.asList(
                        "GET /path1?q1=v1 HTTP/1.1"));
    }

    @Test
    public void testEnabled2ReroutesOriginalAndFinalRequestLine() {
        test(
                true,
                Arrays.asList("%<r %r", "%{<REQUEST_LINE} %{REQUEST_LINE}"),
                Optional.empty(),
                HttpMethod.GET,
                HttpVersion.HTTP_1_1,
                "https",
                new HostAndPortImpl("example.org", 443),
                "/path1",
                "q1=v1",
                Arrays.asList(
                        new Reroute(null, "/path2", "q2=v2"),
                        new Reroute(null, "/path3", "q3=v3")),
                Arrays.asList(
                        "GET /path1?q1=v1 HTTP/1.1 GET /path3?q3=v3 HTTP/1.1"));
    }

    @Test
    public void testEnabled2ReroutesAllOriginalAndFinal() {
        test(
                true,
                Arrays.asList(
                        "%<r %r %<R %R %<q %q %{<BARE_QUERY_STRING} %{BARE_QUERY_STRING} %{<q,q} %{q,q} %<U %U",
                        "%{<REQUEST_LINE} %{REQUEST_LINE} %{<REQUEST_PATH} %{REQUEST_PATH} %{<QUERY_STRING} %{QUERY_STRING} %{<BARE_QUERY_STRING} %{BARE_QUERY_STRING} %{<q,q} %{q,q} %{<REQUEST_URL} %{REQUEST_URL}"),
                Optional.empty(),
                HttpMethod.GET,
                HttpVersion.HTTP_1_1,
                "https",
                new HostAndPortImpl("example.org", 443),
                "/path1",
                "q=v1",
                Arrays.asList(
                        new Reroute(null, "/path2", "q=v2"),
                        new Reroute(null, "/path3", "q=v3")),
                Arrays.asList(
                        "GET /path1?q=v1 HTTP/1.1 GET /path3?q=v3 HTTP/1.1 /path1 /path3 ?q=v1 ?q=v3 q=v1 q=v3 v1 v3 /path1?q=v1 /path3?q=v3"));
    }

    private void test(
            boolean consolidateReroutedRequests,
            List<String> equivalentPatterns,
            Optional<String> excludePattern,
            HttpMethod httpMethod,
            HttpVersion httpVersion,
            String scheme,
            HostAndPort authority,
            String path,
            String query,
            List<Reroute> reroutes,
            List<String> expectedAccessLogEntries) {
        for (String pattern : equivalentPatterns) {
            test(consolidateReroutedRequests, pattern, excludePattern, httpMethod, httpVersion, scheme, authority, path, query,
                    reroutes, expectedAccessLogEntries);
        }
    }

    private void test(
            boolean consolidateReroutedRequests,
            String pattern,
            Optional<String> excludePattern,
            HttpMethod httpMethod,
            HttpVersion httpVersion,
            String scheme,
            HostAndPort authority,
            String path,
            String query,
            List<Reroute> reroutes,
            List<String> expectedAccessLogEntries) {
        List<String> accessLogEntries = new ArrayList<>();
        AccessLogReceiver receiver = new AccessLogReceiver() {
            @Override
            public void logMessage(String message) {
                accessLogEntries.add(message);
            }
        };

        AccessLogHandler accessLogHandler = new AccessLogHandler(receiver, pattern, consolidateReroutedRequests,
                getClass().getClassLoader(), excludePattern);
        QuarkusRequestWrapper request = Mockito.mock(QuarkusRequestWrapper.class);
        Mockito.when(request.getCookie(QuarkusRequestWrapper.FAKE_COOKIE_NAME)).thenReturn(request.new QuarkusCookie());
        Mockito.when(request.version()).thenReturn(httpVersion);
        Mockito.when(request.scheme()).thenReturn(scheme);
        Mockito.when(request.authority()).thenReturn(authority);
        Mockito.when(request.method()).thenReturn(httpMethod);
        Mockito.when(request.path()).thenReturn(path);
        Mockito.when(request.query()).thenReturn(query);
        String uri = uriFromPathAndQuery(path, query);
        Mockito.when(request.uri()).thenReturn(uri);

        HttpServerResponse response = Mockito.mock(HttpServerResponse.class);
        Mockito.when(request.response()).thenReturn(response);

        List<Handler<Void>> requestDoneHandlers = new ArrayList<>();
        Mockito.doAnswer(i -> {
            requestDoneHandlers.add(i.getArgument(0));
            return null;
        }).when(request).addRequestDoneHandler(any());

        RoutingContextImpl rc = Mockito.mock(RoutingContextImpl.class);
        Mockito.when(rc.queryParams()).thenReturn(decodeQueryParams(uri));
        Mockito.when(rc.get(anyString())).thenCallRealMethod();
        Mockito.when(rc.get(anyString(), any())).thenCallRealMethod();
        Mockito.when(rc.put(anyString(), any())).thenCallRealMethod();
        Mockito.when(rc.request()).thenReturn(request);

        accessLogHandler.handle(rc);
        for (Reroute reroute : reroutes) {
            reroute.apply(rc, request);
            accessLogHandler.handle(rc);
        }
        for (Handler<Void> requestDoneHandler : requestDoneHandlers) {
            requestDoneHandler.handle(null);
        }
        Assertions.assertEquals(expectedAccessLogEntries, accessLogEntries);
    }

    private static MultiMap decodeQueryParams(String uri) {
        try {
            MultiMap queryParams = MultiMap.caseInsensitiveMultiMap();
            Map<String, List<String>> decodedParams = new QueryStringDecoder(uri).parameters();
            for (Map.Entry<String, List<String>> entry : decodedParams.entrySet()) {
                queryParams.add(entry.getKey(), entry.getValue());
            }
            return queryParams;
        } catch (IllegalArgumentException e) {
            throw new HttpException(400, "Error while decoding query params", e);
        }
    }

    private static String uriFromPathAndQuery(String path, String query) {
        String uri = null;
        if (path != null)
            uri = path;
        if (query != null)
            uri += "?" + query;
        return uri;
    }

    private static class Reroute {
        private HttpMethod httpMethod;
        private String path;
        private String query;

        public Reroute(HttpMethod httpMethod, String path, String query) {
            this.httpMethod = httpMethod;
            this.path = path;
            this.query = query;
        }

        public void apply(RoutingContextImpl rc, QuarkusRequestWrapper request) {
            if (httpMethod != null)
                Mockito.when(request.method()).thenReturn(httpMethod);
            if (path != null)
                Mockito.when(request.path()).thenReturn(path);
            if (query != null)
                Mockito.when(request.query()).thenReturn(query);
            String uri = uriFromPathAndQuery(path, query);
            if (uri != null) {
                Mockito.when(request.uri()).thenReturn(uri);
                Mockito.when(rc.queryParams()).thenReturn(decodeQueryParams(uri));
            }
        }
    }

}
