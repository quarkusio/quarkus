package io.quarkus.grpc.auth;

import static com.example.security.Security.ThreadInfo.newBuilder;
import static io.quarkus.grpc.auth.BlockingHttpSecurityPolicy.BLOCK_REQUEST;
import static io.quarkus.security.spi.runtime.AuthorizationSuccessEvent.AUTHORIZATION_CONTEXT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.security.SecuredService;
import com.example.security.Security;

import io.grpc.Metadata;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcClientUtils;
import io.quarkus.grpc.GrpcService;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.runtime.interceptor.check.RolesAllowedCheck;
import io.quarkus.security.spi.runtime.AuthenticationSuccessEvent;
import io.quarkus.security.spi.runtime.AuthorizationFailureEvent;
import io.quarkus.security.spi.runtime.AuthorizationSuccessEvent;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;

public abstract class GrpcAuthTestBase {

    public static final Metadata.Key<String> AUTHORIZATION = Metadata.Key.of("Authorization",
            Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> BLOCK_REQUEST_KEY = Metadata.Key.of(BLOCK_REQUEST,
            Metadata.ASCII_STRING_MARSHALLER);
    private static final String PROPS = "quarkus.security.users.embedded.enabled=true\n" +
            "quarkus.security.users.embedded.users.john=john\n" +
            "quarkus.security.users.embedded.roles.john=employees\n" +
            "quarkus.security.users.embedded.users.paul=paul\n" +
            "quarkus.security.users.embedded.roles.paul=interns\n" +
            "quarkus.security.users.embedded.plain-text=true\n" +
            "quarkus.http.auth.basic=true\n";

    protected static QuarkusUnitTest createQuarkusUnitTest(String extraProperty, boolean useGrpcAuthMechanism) {
        return new QuarkusUnitTest().setArchiveProducer(
                () -> {
                    var props = PROPS;
                    if (extraProperty != null) {
                        props += extraProperty;
                    }
                    var jar = ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Service.class, BlockingHttpSecurityPolicy.class, SecurityEventObserver.class)
                            .addPackage(SecuredService.class.getPackage())
                            .add(new StringAsset(props), "application.properties");
                    return useGrpcAuthMechanism ? jar.addClass(BasicGrpcSecurityMechanism.class) : jar;
                });
    }

    public static final String JOHN_BASIC_CREDS = "am9objpqb2hu";
    public static final String PAUL_BASIC_CREDS = "cGF1bDpwYXVs";

    @GrpcClient
    SecuredService securityClient;

    @Inject
    SecurityEventObserver securityEventObserver;

    @BeforeEach
    void clearEvents() {
        securityEventObserver.getStorage().clear();
    }

    @Test
    void shouldSecureUniEndpoint() {
        Metadata headers = new Metadata();
        headers.put(AUTHORIZATION, "Basic " + JOHN_BASIC_CREDS);
        SecuredService client = GrpcClientUtils.attachHeaders(securityClient, headers);
        AtomicInteger resultCount = new AtomicInteger();
        client.unaryCall(Security.Container.newBuilder().setText("woo-hoo").build())
                .subscribe().with(e -> {
                    if (!e.getIsOnEventLoop()) {
                        Assertions.fail("Secured method should be run on event loop");
                    }
                    resultCount.incrementAndGet();
                });

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> resultCount.get() == 1);
        assertSecurityEventsFired("john");
    }

    @Test
    void shouldSecureBlockingUniEndpoint() {
        Metadata headers = new Metadata();
        headers.put(AUTHORIZATION, "Basic " + JOHN_BASIC_CREDS);
        SecuredService client = GrpcClientUtils.attachHeaders(securityClient, headers);
        AtomicInteger resultCount = new AtomicInteger();
        client.unaryCallBlocking(Security.Container.newBuilder().setText("woo-hoo").build())
                .subscribe().with(e -> {
                    if (e.getIsOnEventLoop()) {
                        Assertions.fail("Secured method annotated with @Blocking should be executed on worker thread");
                    }
                    resultCount.incrementAndGet();
                });

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> resultCount.get() == 1);
        assertSecurityEventsFired("john");
    }

    @Test
    void shouldSecureMultiEndpoint() {
        Metadata headers = new Metadata();
        headers.put(AUTHORIZATION, "Basic " + PAUL_BASIC_CREDS);
        SecuredService client = GrpcClientUtils.attachHeaders(securityClient, headers);
        List<Boolean> results = new CopyOnWriteArrayList<>();
        client.streamCall(Multi.createBy().repeating()
                .supplier(() -> (Security.Container.newBuilder().setText("woo-hoo").build())).atMost(4))
                .subscribe().with(e -> results.add(e.getIsOnEventLoop()));

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> results.size() == 5);

        assertThat(results.stream().filter(e -> !e)).isEmpty();
        assertSecurityEventsFired("paul");
    }

    @Test
    void shouldSecureBlockingMultiEndpoint() {
        Metadata headers = new Metadata();
        headers.put(AUTHORIZATION, "Basic " + PAUL_BASIC_CREDS);
        SecuredService client = GrpcClientUtils.attachHeaders(securityClient, headers);
        List<Boolean> results = new CopyOnWriteArrayList<>();
        client.streamCallBlocking(Multi.createBy().repeating()
                .supplier(() -> (Security.Container.newBuilder().setText("woo-hoo").build())).atMost(4))
                .subscribe().with(e -> results.add(e.getIsOnEventLoop()));

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> results.size() == 5);

        assertThat(results.stream().filter(e -> e)).isEmpty();
        assertSecurityEventsFired("paul");
    }

    @Test
    void shouldFailWithInvalidCredentials() {
        Metadata headers = new Metadata();
        headers.put(AUTHORIZATION, "Basic invalid creds");
        SecuredService client = GrpcClientUtils.attachHeaders(securityClient, headers);

        AtomicReference<Throwable> error = new AtomicReference<>();

        AtomicInteger resultCount = new AtomicInteger();
        client.unaryCall(Security.Container.newBuilder().setText("woo-hoo").build())
                .onFailure().invoke(error::set)
                .subscribe().with(e -> resultCount.incrementAndGet());

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> error.get() != null);
    }

    @Test
    void shouldFailWithInvalidInsufficientRole() {
        Metadata headers = new Metadata();
        headers.put(AUTHORIZATION, PAUL_BASIC_CREDS);
        SecuredService client = GrpcClientUtils.attachHeaders(securityClient, headers);

        AtomicReference<Throwable> error = new AtomicReference<>();

        AtomicInteger resultCount = new AtomicInteger();
        client.unaryCall(Security.Container.newBuilder().setText("woo-hoo").build())
                .onFailure().invoke(error::set)
                .subscribe().with(e -> resultCount.incrementAndGet());

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> error.get() != null);

        // we don't check exact count as HTTP Security policies are not supported when gRPC is running on separate server
        assertFalse(securityEventObserver.getStorage().isEmpty());
        // fails RolesAllowed check as the anonymous identity has no roles
        AuthorizationFailureEvent event = (AuthorizationFailureEvent) securityEventObserver
                .getStorage().get(securityEventObserver.getStorage().size() - 1);
        assertNotNull(event.getSecurityIdentity());
        assertTrue(event.getSecurityIdentity().isAnonymous());
        assertInstanceOf(UnauthorizedException.class, event.getAuthorizationFailure());
        assertEquals(RolesAllowedCheck.class.getName(), event.getAuthorizationContext());
    }

    @Test
    void shouldSecureUniEndpointWithBlockingHttpSecurityPolicy() {
        Metadata headers = new Metadata();
        headers.put(AUTHORIZATION, "Basic " + JOHN_BASIC_CREDS);
        addBlockingHeaders(headers);
        SecuredService client = GrpcClientUtils.attachHeaders(securityClient, headers);
        AtomicInteger resultCount = new AtomicInteger();
        client.unaryCall(Security.Container.newBuilder().setText("woo-hoo").build())
                .subscribe().with(e -> {
                    if (!e.getIsOnEventLoop()) {
                        Assertions.fail("Secured method should be run on event loop");
                    }
                    resultCount.incrementAndGet();
                });

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> resultCount.get() == 1);
        assertSecurityEventsFired("john");
    }

    @Test
    void shouldSecureBlockingUniEndpointWithBlockingHttpSecurityPolicy() {
        Metadata headers = new Metadata();
        headers.put(AUTHORIZATION, "Basic " + JOHN_BASIC_CREDS);
        addBlockingHeaders(headers);
        SecuredService client = GrpcClientUtils.attachHeaders(securityClient, headers);
        AtomicInteger resultCount = new AtomicInteger();
        client.unaryCallBlocking(Security.Container.newBuilder().setText("woo-hoo").build())
                .subscribe().with(e -> {
                    if (e.getIsOnEventLoop()) {
                        Assertions.fail("Secured method annotated with @Blocking should be executed on worker thread");
                    }
                    resultCount.incrementAndGet();
                });

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> resultCount.get() == 1);
        assertSecurityEventsFired("john");
    }

    @Test
    void shouldSecureMultiEndpointWithBlockingHttpSecurityPolicy() {
        Metadata headers = new Metadata();
        headers.put(AUTHORIZATION, "Basic " + PAUL_BASIC_CREDS);
        addBlockingHeaders(headers);
        SecuredService client = GrpcClientUtils.attachHeaders(securityClient, headers);
        List<Boolean> results = new CopyOnWriteArrayList<>();
        client.streamCall(Multi.createBy().repeating()
                .supplier(() -> (Security.Container.newBuilder().setText("woo-hoo").build())).atMost(4))
                .subscribe().with(e -> {
                    results.add(e.getIsOnEventLoop());
                });

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> results.size() == 5);

        assertThat(results.stream().filter(e -> !e)).isEmpty();
        assertSecurityEventsFired("paul");
    }

    @Test
    void shouldSecureBlockingMultiEndpointWithBlockingHttpSecurityPolicy() {
        Metadata headers = new Metadata();
        headers.put(AUTHORIZATION, "Basic " + PAUL_BASIC_CREDS);
        addBlockingHeaders(headers);
        SecuredService client = GrpcClientUtils.attachHeaders(securityClient, headers);
        List<Boolean> results = new CopyOnWriteArrayList<>();
        client.streamCallBlocking(Multi.createBy().repeating()
                .supplier(() -> (Security.Container.newBuilder().setText("woo-hoo").build())).atMost(4))
                .subscribe().with(e -> results.add(e.getIsOnEventLoop()));

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> results.size() == 5);

        assertThat(results.stream().filter(e -> e)).isEmpty();
        assertSecurityEventsFired("paul");
    }

    private void assertSecurityEventsFired(String username) {
        // expect at least authentication success and RolesAllowed security check success
        // we don't check exact count as HTTP Security policies are not supported when gRPC is running on separate server
        assertTrue(securityEventObserver.getStorage().size() >= 2);
        assertTrue(securityEventObserver.getStorage().stream().anyMatch(e -> e instanceof AuthenticationSuccessEvent));
        AuthorizationSuccessEvent event = (AuthorizationSuccessEvent) securityEventObserver.getStorage()
                .get(securityEventObserver.getStorage().size() - 1);
        assertNotNull(event.getSecurityIdentity());
        assertEquals(username, event.getSecurityIdentity().getPrincipal().getName());
        assertEquals(RolesAllowedCheck.class.getName(), event.getEventProperties().get(AUTHORIZATION_CONTEXT));
    }

    private static void addBlockingHeaders(Metadata headers) {
        headers.put(BLOCK_REQUEST_KEY, "ignored");
    }

    @GrpcService
    public static class Service implements SecuredService {
        @Override
        @RolesAllowed("employees")
        public Uni<Security.ThreadInfo> unaryCall(Security.Container request) {
            return Uni.createFrom()
                    .item(newBuilder().setIsOnEventLoop(Context.isOnEventLoopThread()).build());
        }

        @Override
        @RolesAllowed("interns")
        public Multi<Security.ThreadInfo> streamCall(Multi<Security.Container> request) {
            return Multi.createBy()
                    .repeating().supplier(() -> newBuilder().setIsOnEventLoop(Context.isOnEventLoopThread()).build())
                    .atMost(5);
        }

        @Blocking
        @Override
        @RolesAllowed("employees")
        public Uni<Security.ThreadInfo> unaryCallBlocking(Security.Container request) {
            return Uni.createFrom()
                    .item(newBuilder().setIsOnEventLoop(Context.isOnEventLoopThread()).build());
        }

        @Blocking
        @Override
        @RolesAllowed("interns")
        public Multi<Security.ThreadInfo> streamCallBlocking(Multi<Security.Container> request) {
            return Multi.createBy()
                    .repeating().supplier(() -> newBuilder().setIsOnEventLoop(Context.isOnEventLoopThread()).build())
                    .atMost(5);
        }
    }
}
