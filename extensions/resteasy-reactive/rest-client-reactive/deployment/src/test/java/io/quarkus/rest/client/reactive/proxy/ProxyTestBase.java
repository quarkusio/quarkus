package io.quarkus.rest.client.reactive.proxy;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.net.URI;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.quarkus.test.common.http.TestHTTPResource;

public abstract class ProxyTestBase {

    public static final String NO_PROXY = "noProxy";

    public static final String PROXY_8181 = "proxyServer1";
    public static final String PROXY_8182 = "proxyServer2";
    public static final String AUTHENTICATED_PROXY = "authenticatedProxy";
    @TestHTTPResource
    URI appUri;

    private WireMockServer proxyServer1;
    private WireMockServer proxyServer2;
    private WireMockServer authenticatedProxyServer;

    @BeforeEach
    public void setUp() {
        proxyServer1 = new WireMockServer(8181);
        proxyServer1.stubFor(WireMock.get(urlMatching("/.*"))
                .willReturn(aResponse().proxiedFrom("http://localhost:" + appUri.getPort())
                        .withAdditionalRequestHeader("X-Via", PROXY_8181)));
        proxyServer1.start();
        proxyServer2 = new WireMockServer(8182);
        proxyServer2.stubFor(WireMock.get(urlMatching("/.*"))
                .willReturn(aResponse().proxiedFrom("http://localhost:" + appUri.getPort())
                        .withAdditionalRequestHeader("X-Via", PROXY_8182)));
        proxyServer2.start();
        authenticatedProxyServer = new WireMockServer(8183);
        authenticatedProxyServer
                .stubFor(WireMock.get(urlMatching("/.*")).withHeader("Proxy-Authorization", equalTo("Basic YWRtaW46cjAwdA=="))
                        .willReturn(aResponse().proxiedFrom("http://localhost:" + appUri.getPort())
                                .withAdditionalRequestHeader("X-Via", AUTHENTICATED_PROXY)));
        authenticatedProxyServer.start();
    }

    @AfterEach
    public void shutdown() {
        proxyServer1.stop();
        proxyServer2.stop();
        authenticatedProxyServer.stop();
    }
}
