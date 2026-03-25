package io.quarkus.vertx.http.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.impl.HttpServerRequestInternal;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.net.HostAndPort;
import io.vertx.core.net.SocketAddress;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ForwardedServerRequestWrapperTest {

    private HttpServerRequestInternal mockRequest;
    private ForwardedServerRequestWrapper wrapper;

    @BeforeEach
    void setUp() {
        mockRequest = Mockito.mock(HttpServerRequestInternal.class, Answers.RETURNS_DEEP_STUBS);

        MultiMap headers = new HeadersMultiMap();
        when(mockRequest.headers()).thenReturn(headers);
        when(mockRequest.scheme()).thenReturn("http");
        when(mockRequest.host()).thenReturn("localhost");
        when(mockRequest.uri()).thenReturn("/original");
        when(mockRequest.method()).thenReturn(HttpMethod.GET);
        when(mockRequest.path()).thenReturn("/original");
        when(mockRequest.query()).thenReturn(null);
        when(mockRequest.remoteAddress()).thenReturn(SocketAddress.inetSocketAddress(8080, "127.0.0.1"));
        when(mockRequest.authority()).thenReturn(HostAndPort.create("localhost", 8080));

        ForwardingProxyOptions options = new ForwardingProxyOptions(
                false, false, false, false, false, null, false, false, null, null, null);

        TrustedProxyCheck trustedProxyCheck = TrustedProxyCheck.allowAll();
        wrapper = new ForwardedServerRequestWrapper(mockRequest, options, trustedProxyCheck);
    }

    @Test
    void changeToWithQueryString() {
        wrapper.changeTo(HttpMethod.POST, "/foo?bar=baz");

        assertEquals("/foo", wrapper.path());
        assertEquals("bar=baz", wrapper.query());
    }

    @Test
    void changeToWithFragmentOnly() {
        wrapper.changeTo(HttpMethod.GET, "/foo#section");

        assertEquals("/foo", wrapper.path());
        assertNull(wrapper.query());
    }

    @Test
    void changeToWithQueryAndFragment() {
        wrapper.changeTo(HttpMethod.PUT, "/foo?bar=baz#section");

        assertEquals("/foo", wrapper.path());
        assertEquals("bar=baz", wrapper.query());
    }

    @Test
    void changeToWithPlainPath() {
        wrapper.changeTo(HttpMethod.DELETE, "/plain/path");

        assertEquals("/plain/path", wrapper.path());
        assertNull(wrapper.query());
        assertEquals("/plain/path", wrapper.uri());
    }

    @Test
    void changeToUpdatesMethod() {
        wrapper.changeTo(HttpMethod.POST, "/any");
        assertEquals(HttpMethod.POST, wrapper.method());
    }

    @Test
    void changeToUpdatesUri() {
        wrapper.changeTo(HttpMethod.PATCH, "/new/uri?key=val");
        assertEquals("/new/uri?key=val", wrapper.uri());
    }

    @Test
    void withoutChangeToMethodDelegatesToRequest() {
        assertEquals(HttpMethod.GET, wrapper.method());
    }

    @Test
    void withoutChangeToUriDelegatesToForwardedParser() {
        assertEquals("/original", wrapper.uri());
    }

    @Test
    void withoutChangeToPathDelegatesToRequest() {
        assertEquals("/original", wrapper.path());
    }

    @Test
    void withoutChangeToQueryDelegatesToRequest() {
        assertNull(wrapper.query());
    }

    @Test
    void absoluteURIWhenModifiedWithSchemeAndHost() {
        wrapper.changeTo(HttpMethod.GET, "/new/path?q=1");

        String absoluteURI = wrapper.absoluteURI();

        assertEquals("http://localhost/new/path?q=1", absoluteURI);
    }

    @Test
    void absoluteURIWhenNotModifiedDelegatesToForwardedParser() {
        String absoluteURI = wrapper.absoluteURI();
        assertEquals("http://localhost/original", absoluteURI);
    }

    @Test
    void authorityWithRealTrueReturnsDelegateAuthority() {
        HostAndPort delegateAuthority = HostAndPort.create("delegate-host", 9090);
        when(mockRequest.authority()).thenReturn(delegateAuthority);

        HostAndPort result = wrapper.authority(true);

        assertEquals("delegate-host", result.host());
        assertEquals(9090, result.port());
    }

    @Test
    void authorityWithRealFalseReturnsForwardedAuthority() {
        HostAndPort result = wrapper.authority(false);
        assertEquals("localhost", result.host());
    }

    @Test
    void isValidAuthorityReturnsTrueWhenAuthorityIsNotNull() {
        assertTrue(wrapper.isValidAuthority());
    }

    @Test
    void schemeReturnsDelegateScheme() {
        assertEquals("http", wrapper.scheme());
    }

    @Test
    void hostReturnsForwardedParserHost() {
        assertEquals("localhost", wrapper.host());
    }

    @Test
    void changeToMultipleTimesUsesLatestValues() {
        wrapper.changeTo(HttpMethod.POST, "/first?a=1");
        wrapper.changeTo(HttpMethod.DELETE, "/second#frag");

        assertEquals(HttpMethod.DELETE, wrapper.method());
        assertEquals("/second#frag", wrapper.uri());
        assertEquals("/second", wrapper.path());
        assertNull(wrapper.query());
    }

}
