package io.quarkus.websockets.next.test.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.BaseAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.quarkus.vertx.http.runtime.security.annotation.BasicAuthentication;
import io.quarkus.vertx.http.runtime.security.annotation.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.annotation.MTLSAuthentication;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.PathParam;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketClient;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketConnector;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.UpgradeRejectedException;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.web.RoutingContext;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "mtls-test", password = "secret", formats = Format.PKCS12, client = true))
public class HttpUpgradeSelectAuthMechWithAnnotationTest extends SecurityTestBase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("""
                            quarkus.tls.key-store.p12.path=keystore.p12
                            quarkus.tls.key-store.p12.password=secret
                            quarkus.tls.trust-store.p12.path=server-truststore.p12
                            quarkus.tls.trust-store.p12.password=secret
                            quarkus.tls.ws-client.trust-store.p12.path=client-truststore.p12
                            quarkus.tls.ws-client.trust-store.p12.password=secret
                            quarkus.tls.ws-client.key-store.p12.path=client-keystore.p12
                            quarkus.tls.ws-client.key-store.p12.password=secret
                            quarkus.websockets-next.client.tls-configuration-name=ws-client
                            quarkus.http.auth.proactive=false
                            quarkus.http.ssl.client-auth=request
                            quarkus.http.insecure-requests=enabled
                            quarkus.http.auth.basic=true
                            """), "application.properties")
                    .addAsResource(new File("target/certs/mtls-test-client-keystore.p12"), "client-keystore.p12")
                    .addAsResource(new File("target/certs/mtls-test-keystore.p12"), "keystore.p12")
                    .addAsResource(new File("target/certs/mtls-test-server-truststore.p12"), "server-truststore.p12")
                    .addAsResource(new File("target/certs/mtls-test-client-truststore.p12"), "client-truststore.p12")
                    .addClasses(Endpoint.class, WSClient.class, TestIdentityProvider.class, TestIdentityController.class,
                            PublicEndpoint.class, PublicEndpoint.SubEndpoint.class, CustomAuthEndpoint.class,
                            CustomAuthenticationRequest.class, CustomAuthMechanism.class, CustomIdentityProvider.class,
                            RolesAndCustomAuthEndpoint.class, UnknownAuthEndpoint.class));

    @Inject
    WebSocketConnector<TlsClientEndpoint> connector;

    @TestHTTPResource(value = "/", tls = true)
    URI tlsEndpointUri;

    @TestHTTPResource(value = "/")
    URI tlsEndpointUnsecuredUri;

    @TestHTTPResource("unknown-auth-endpoint")
    URI unknownAuthEndpointUri;

    @TestHTTPResource("public-end")
    URI publicEndUri;

    @TestHTTPResource("public-end/sub")
    URI subEndUri;

    @TestHTTPResource("custom-auth-endpoint")
    URI customAuthEndointUri;

    @TestHTTPResource("roles-and-custom-auth-endpoint")
    URI rolesAndCustomAuthEndpointUri;

    @Test
    public void testBasicAuthSubEndpoint() {
        // no authentication required - public endpoint
        try (WSClient client = new WSClient(vertx)) {
            client.connect(publicEndUri);
            client.waitForMessages(1);
            assertEquals("ready", client.getMessages().get(0).toString());
            client.sendAndAwait("hello");
            client.waitForMessages(2);
            assertEquals("hello", client.getMessages().get(1).toString());
        }

        // authentication failure as no basic auth is required and no credentials were provided
        try (WSClient client = new WSClient(vertx)) {
            CompletionException ce = assertThrows(CompletionException.class, () -> client.connect(subEndUri));
            Throwable root = ExceptionUtil.getRootCause(ce);
            assertInstanceOf(UpgradeRejectedException.class, root);
            assertTrue(root.getMessage().contains("401"), root.getMessage());
        }
        // basic authentication - succeed as admin has required permissions
        try (WSClient client = new WSClient(vertx)) {
            client.connect(basicAuth("admin", "admin"), subEndUri);
            client.waitForMessages(1);
            assertEquals("ready", client.getMessages().get(0).toString());
            client.sendAndAwait("hello");
            client.waitForMessages(2);
            assertEquals("sub-endpoint", client.getMessages().get(1).toString());
        }
        // basic authentication - fail as user doesn't have 'admin' role
        try (WSClient client = new WSClient(vertx)) {
            client.connect(basicAuth("user", "user"), subEndUri);
            client.waitForMessages(1);
            assertEquals("ready", client.getMessages().get(0).toString());
            client.sendAndAwait("hello");
            client.waitForMessages(2);
            assertEquals("sub-endpoint:forbidden:user", client.getMessages().get(1).toString());
        }
        // custom authentication - correct credentials - fails as wrong authentication mechanism
        try (WSClient client = new WSClient(vertx)) {
            CompletionException ce = assertThrows(CompletionException.class,
                    () -> client.connect(customAuth("admin"), subEndUri));
            Throwable root = ExceptionUtil.getRootCause(ce);
            assertInstanceOf(UpgradeRejectedException.class, root);
            assertTrue(root.getMessage().contains("401"), root.getMessage());
        }
    }

    @Test
    public void testCustomAuthenticationEndpoint() {
        // no credentials - deny access
        try (WSClient client = new WSClient(vertx)) {
            CompletionException ce = assertThrows(CompletionException.class, () -> client.connect(customAuthEndointUri));
            Throwable root = ExceptionUtil.getRootCause(ce);
            assertInstanceOf(UpgradeRejectedException.class, root);
            assertTrue(root.getMessage().contains("401"));
        }
        // custom auth - correct credentials - pass as has admin role
        try (WSClient client = new WSClient(vertx)) {
            client.connect(customAuth("admin"), customAuthEndointUri);
            client.waitForMessages(1);
            assertEquals("ready", client.getMessages().get(0).toString());
            client.sendAndAwait("hello");
            client.waitForMessages(2);
            assertEquals("hello who", client.getMessages().get(1).toString());
        }
        // basic auth - correct credentials - fail as wrong mechanism
        try (WSClient client = new WSClient(vertx)) {
            CompletionException ce = assertThrows(CompletionException.class, () -> client
                    .connect(basicAuth("admin", "admin"), customAuthEndointUri));
            Throwable root = ExceptionUtil.getRootCause(ce);
            assertInstanceOf(UpgradeRejectedException.class, root);
            assertTrue(root.getMessage().contains("401"), root.getMessage());
        }
    }

    @Test
    public void testUnknownAuthenticationEndpoint() {
        // there is no such authentication mechanism, therefore all requests must be denied
        // we can't validate it during the build-time as credentials transport is resolved based on the RoutingContext

        // no credentials - deny access
        try (WSClient client = new WSClient(vertx)) {
            CompletionException ce = assertThrows(CompletionException.class, () -> client.connect(unknownAuthEndpointUri));
            Throwable root = ExceptionUtil.getRootCause(ce);
            assertInstanceOf(UpgradeRejectedException.class, root);
            assertTrue(root.getMessage().contains("401"));
        }
        // basic auth - correct credentials - fail as wrong mechanism
        try (WSClient client = new WSClient(vertx)) {
            CompletionException ce = assertThrows(CompletionException.class, () -> client
                    .connect(basicAuth("admin", "admin"), unknownAuthEndpointUri));
            Throwable root = ExceptionUtil.getRootCause(ce);
            assertInstanceOf(UpgradeRejectedException.class, root);
            assertTrue(root.getMessage().contains("401"), root.getMessage());
        }
    }

    @Test
    public void testHttpUpgradeRolesAllowedAndCustomAuth() {
        // custom auth - correct credentials - pass as has admin role
        try (WSClient client = new WSClient(vertx)) {
            client.connect(customAuth("admin"), rolesAndCustomAuthEndpointUri);
            client.waitForMessages(1);
            assertEquals("ready", client.getMessages().get(0).toString());
            client.sendAndAwait("hello");
            client.waitForMessages(2);
            assertEquals("hello who", client.getMessages().get(1).toString());
        }
        // custom auth - correct credentials - fails as no admin role
        try (WSClient client = new WSClient(vertx)) {
            CompletionException ce = assertThrows(CompletionException.class, () -> client
                    .connect(customAuth("user"), rolesAndCustomAuthEndpointUri));
            Throwable root = ExceptionUtil.getRootCause(ce);
            assertInstanceOf(UpgradeRejectedException.class, root);
            assertTrue(root.getMessage().contains("403"), root.getMessage());
        }
        // basic auth - correct credentials - fails as wrong authentication mechanism
        try (WSClient client = new WSClient(vertx)) {
            CompletionException ce = assertThrows(CompletionException.class, () -> client
                    .connect(basicAuth("admin", "admin"), rolesAndCustomAuthEndpointUri));
            Throwable root = ExceptionUtil.getRootCause(ce);
            assertInstanceOf(UpgradeRejectedException.class, root);
            assertTrue(root.getMessage().contains("401"), root.getMessage());
        }
    }

    @Test
    public void testMutualTlsEndpoint() throws InterruptedException, URISyntaxException {
        // no TLS, no credentials
        Assertions.assertThrows(UpgradeRejectedException.class, () -> assertTlsClient(tlsEndpointUnsecuredUri, null));
        // no TLS, admin basic auth credentials
        Assertions.assertThrows(UpgradeRejectedException.class, () -> assertTlsClient(tlsEndpointUnsecuredUri, "admin"));
        // authenticated as communication is opening WebSockets handshake request
        assertTlsClient(tlsEndpointUri, null);
        // authenticated using mTLS with explicit 'wss'
        URI wssUri = new URI("wss", tlsEndpointUri.getUserInfo(), tlsEndpointUri.getHost(),
                tlsEndpointUri.getPort(), tlsEndpointUri.getPath(), tlsEndpointUri.getQuery(), tlsEndpointUri.getFragment());
        assertTlsClient(wssUri, null);
    }

    private void assertTlsClient(URI uri, String basicAuthCred) throws InterruptedException {
        TlsServerEndpoint.reset();
        TlsClientEndpoint.reset();

        var connectorBuilder = connector
                .baseUri(uri)
                // The value will be encoded automatically
                .pathParam("name", "Lu=");
        if (basicAuthCred != null) {
            connectorBuilder = connectorBuilder.addHeader(HttpHeaders.AUTHORIZATION.toString(),
                    new UsernamePasswordCredentials(basicAuthCred, basicAuthCred).applyHttpChallenge(null)
                            .toHttpAuthorization());
        }
        WebSocketClientConnection connection = connectorBuilder.connectAndAwait();
        assertTrue(connection.isSecure());

        assertEquals("Lu=", connection.pathParam("name"));
        connection.sendTextAndAwait("Hi!");

        assertTrue(TlsClientEndpoint.messageLatch.await(5, TimeUnit.SECONDS));
        assertEquals("Lu=:Hello Lu=!", TlsClientEndpoint.MESSAGES.get(0));
        assertEquals("Lu=:Hi!", TlsClientEndpoint.MESSAGES.get(1));

        connection.closeAndAwait();
        assertTrue(TlsClientEndpoint.closedLatch.await(5, TimeUnit.SECONDS));
        assertTrue(TlsServerEndpoint.closedLatch.await(5, TimeUnit.SECONDS));
    }

    private static WebSocketConnectOptions customAuth(String role) {
        return new WebSocketConnectOptions().addHeader("CustomAuthorization", role);
    }

    @WebSocket(path = "/public-end")
    public static class PublicEndpoint {

        @OnOpen
        String open() {
            return "ready";
        }

        @OnTextMessage
        String echo(String message) {
            return message;
        }

        @BasicAuthentication
        @WebSocket(path = "/sub")
        public static class SubEndpoint {

            @Inject
            CurrentIdentityAssociation currentIdentity;

            @OnOpen
            String open() {
                return "ready";
            }

            @RolesAllowed("admin")
            @OnTextMessage
            String echo(String message) {
                return "sub-endpoint";
            }

            @OnError
            String error(ForbiddenException t) {
                return "sub-endpoint:forbidden:" + currentIdentity.getIdentity().getPrincipal().getName();
            }
        }

    }

    @BasicAuthentication
    @WebSocket(path = "/end")
    public static class Endpoint {

        @Inject
        CurrentIdentityAssociation currentIdentity;

        @OnOpen
        String open() {
            return "ready";
        }

        @RolesAllowed("admin")
        @OnTextMessage
        String echo(String message) {
            return message;
        }

        @OnError
        String error(ForbiddenException t) {
            return "forbidden:" + currentIdentity.getIdentity().getPrincipal().getName();
        }

    }

    @HttpAuthenticationMechanism("custom")
    @WebSocket(path = "/custom-auth-endpoint")
    public static class CustomAuthEndpoint {

        @Inject
        CurrentIdentityAssociation currentIdentity;

        @OnOpen
        String open() {
            return "ready";
        }

        @OnTextMessage
        String echo(String message) {
            return message + " " + currentIdentity.getIdentity().getPrincipal().getName();
        }

        @OnError
        String error(ForbiddenException t) {
            return "forbidden:" + currentIdentity.getIdentity().getPrincipal().getName();
        }

    }

    @HttpAuthenticationMechanism("unknown")
    @WebSocket(path = "/unknown-auth-endpoint")
    public static class UnknownAuthEndpoint {

        @OnOpen
        String open() {
            return "unreachable";
        }

        @OnTextMessage
        String echo(String message) {
            return "unreachable";
        }

    }

    @RolesAllowed("admin")
    @HttpAuthenticationMechanism("custom")
    @WebSocket(path = "/roles-and-custom-auth-endpoint")
    public static class RolesAndCustomAuthEndpoint {

        @Inject
        CurrentIdentityAssociation currentIdentity;

        @OnOpen
        String open() {
            return "ready";
        }

        @OnTextMessage
        String echo(String message) {
            return message + " " + currentIdentity.getIdentity().getPrincipal().getName();
        }

    }

    public static final class CustomAuthenticationRequest extends BaseAuthenticationRequest {

        private final String role;

        private CustomAuthenticationRequest(String role) {
            this.role = role;
        }
    }

    @ApplicationScoped
    public static class CustomAuthMechanism implements io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism {

        @Override
        public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
            String role = context.request().getHeader("CustomAuthorization");
            if (role != null && !role.isEmpty()) {
                return identityProviderManager.authenticate(new CustomAuthenticationRequest(role));
            }
            return Uni.createFrom().nullItem();
        }

        @Override
        public Uni<ChallengeData> getChallenge(RoutingContext context) {
            return Uni.createFrom().item(new ChallengeData(401, null, null));
        }

        @Override
        public Uni<HttpCredentialTransport> getCredentialTransport(RoutingContext context) {
            return Uni.createFrom().item(new HttpCredentialTransport(HttpCredentialTransport.Type.AUTHORIZATION,
                    "custom", "custom"));
        }
    }

    @ApplicationScoped
    public static class CustomIdentityProvider implements IdentityProvider<CustomAuthenticationRequest> {

        @Override
        public Class<CustomAuthenticationRequest> getRequestType() {
            return CustomAuthenticationRequest.class;
        }

        @Override
        public Uni<SecurityIdentity> authenticate(CustomAuthenticationRequest customAuthenticationRequest,
                AuthenticationRequestContext authenticationRequestContext) {
            return Uni.createFrom().item(QuarkusSecurityIdentity.builder()
                    .setPrincipal(new QuarkusPrincipal("who"))
                    .setAnonymous(false)
                    .addRole(customAuthenticationRequest.role)
                    .build());
        }
    }

    @MTLSAuthentication
    @WebSocket(path = "/tls-endpoint/{name}")
    public static class TlsServerEndpoint {

        static volatile CountDownLatch closedLatch = new CountDownLatch(1);

        @OnOpen
        String open(@PathParam String name) {
            return "Hello " + name + "!";
        }

        @OnTextMessage
        String echo(String message) {
            return message;
        }

        @OnClose
        void close() {
            closedLatch.countDown();
        }

        static void reset() {
            closedLatch = new CountDownLatch(1);
        }

    }

    @WebSocketClient(path = "/tls-endpoint/{name}")
    public static class TlsClientEndpoint {

        static volatile CountDownLatch messageLatch = new CountDownLatch(2);

        static final List<String> MESSAGES = new CopyOnWriteArrayList<>();

        static volatile CountDownLatch closedLatch = new CountDownLatch(1);

        @OnTextMessage
        void onMessage(@PathParam String name, String message, WebSocketClientConnection connection) {
            if (!name.equals(connection.pathParam("name"))) {
                throw new IllegalArgumentException();
            }
            MESSAGES.add(name + ":" + message);
            messageLatch.countDown();
        }

        @OnClose
        void close() {
            closedLatch.countDown();
        }

        static void reset() {
            MESSAGES.clear();
            messageLatch = new CountDownLatch(2);
            closedLatch = new CountDownLatch(1);
        }

    }
}
