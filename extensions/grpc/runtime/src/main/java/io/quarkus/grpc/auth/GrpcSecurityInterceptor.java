package io.quarkus.grpc.auth;

import static io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle.isExplicitlyMarkedAsUnsafe;
import static io.quarkus.vertx.http.runtime.security.QuarkusHttpUser.DEFERRED_IDENTITY_KEY;
import static io.smallrye.common.vertx.VertxContext.isDuplicatedContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.Prioritized;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.quarkus.grpc.GlobalInterceptor;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

/**
 * Security interceptor invoking {@link GrpcSecurityMechanism} implementations
 */
@GlobalInterceptor
@Singleton
public final class GrpcSecurityInterceptor implements ServerInterceptor, Prioritized {

    private static final Logger log = Logger.getLogger(GrpcSecurityInterceptor.class);
    private static final String IDENTITY_KEY = "io.quarkus.grpc.auth.identity";

    private final IdentityProviderManager identityProviderManager;
    private final CurrentIdentityAssociation identityAssociation;

    private final AuthExceptionHandlerProvider exceptionHandlerProvider;
    private final List<GrpcSecurityMechanism> securityMechanisms;

    private final Map<String, List<String>> serviceToBlockingMethods = new HashMap<>();
    private boolean hasBlockingMethods = false;
    private final boolean notUsingSeparateGrpcServer;

    @Inject
    public GrpcSecurityInterceptor(
            CurrentIdentityAssociation identityAssociation,
            IdentityProviderManager identityProviderManager,
            Instance<GrpcSecurityMechanism> securityMechanisms,
            Instance<AuthExceptionHandlerProvider> exceptionHandlers,
            @ConfigProperty(name = "quarkus.grpc.server.use-separate-server") boolean usingSeparateGrpcServer) {
        this.identityAssociation = identityAssociation;
        this.identityProviderManager = identityProviderManager;
        this.notUsingSeparateGrpcServer = !usingSeparateGrpcServer;

        AuthExceptionHandlerProvider maxPrioHandlerProvider = null;

        for (AuthExceptionHandlerProvider handler : exceptionHandlers) {
            if (maxPrioHandlerProvider == null || maxPrioHandlerProvider.getPriority() < handler.getPriority()) {
                maxPrioHandlerProvider = handler;
            }
        }
        this.exceptionHandlerProvider = maxPrioHandlerProvider;

        List<GrpcSecurityMechanism> mechanisms = new ArrayList<>();
        for (GrpcSecurityMechanism securityMechanism : securityMechanisms) {
            mechanisms.add(securityMechanism);
        }
        if (mechanisms.isEmpty()) {
            this.securityMechanisms = null;
        } else {
            mechanisms.sort(Comparator.comparing(GrpcSecurityMechanism::getPriority));
            this.securityMechanisms = mechanisms;
        }
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall,
            Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {
        boolean identityAssociationNotSet = true;
        if (securityMechanisms != null) {
            Exception error = null;
            for (GrpcSecurityMechanism securityMechanism : securityMechanisms) {
                if (securityMechanism.handles(metadata)) {
                    try {
                        AuthenticationRequest authenticationRequest = securityMechanism.createAuthenticationRequest(metadata);
                        Context context = Vertx.currentContext();
                        boolean onEventLoopThread = Context.isOnEventLoopThread();

                        final boolean isBlockingMethod;
                        if (hasBlockingMethods) {
                            var methods = serviceToBlockingMethods.get(serverCall.getMethodDescriptor().getServiceName());
                            if (methods != null) {
                                isBlockingMethod = methods.contains(serverCall.getMethodDescriptor().getFullMethodName());
                            } else {
                                isBlockingMethod = false;
                            }
                        } else {
                            isBlockingMethod = false;
                        }

                        if (authenticationRequest != null) {
                            Uni<SecurityIdentity> auth = identityProviderManager
                                    .authenticate(authenticationRequest)
                                    .emitOn(new Executor() {
                                        @Override
                                        public void execute(Runnable command) {
                                            if (onEventLoopThread && !isBlockingMethod) {
                                                context.runOnContext(new Handler<>() {
                                                    @Override
                                                    public void handle(Void event) {
                                                        command.run();
                                                    }
                                                });
                                            } else {
                                                command.run();
                                            }
                                        }
                                    });
                            identityAssociation.setIdentity(auth);
                            error = null;
                            identityAssociationNotSet = false;
                            break;
                        }
                    } catch (Exception e) {
                        error = e;
                        log.warn("Failed to prepare AuthenticationRequest for a gRPC call", e);
                    }
                }
            }
            if (error != null) { // if parsing for all security mechanisms failed, let's propagate the last exception
                identityAssociation.setIdentity(Uni.createFrom()
                        .failure(new AuthenticationFailedException("Failed to parse authentication data", error)));
            }
        }
        if (identityAssociationNotSet && notUsingSeparateGrpcServer) {
            // authenticate via HTTP authenticator
            Context capturedContext = getCapturedVertxContext();
            if (capturedContext != null) {
                if (capturedContext.getLocal(IDENTITY_KEY) != null) {
                    identityAssociation.setIdentity(capturedContext.<SecurityIdentity> getLocal(IDENTITY_KEY));
                } else if (capturedContext.getLocal(DEFERRED_IDENTITY_KEY) != null) {
                    identityAssociation.setIdentity(capturedContext.<Uni<SecurityIdentity>> getLocal(DEFERRED_IDENTITY_KEY));
                }
            }
        }
        ServerCall.Listener<ReqT> listener = serverCallHandler.startCall(serverCall, metadata);
        return exceptionHandlerProvider.createHandler(listener, serverCall, metadata);
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE - 100;
    }

    void init(Map<String, List<String>> serviceToBlockingMethods) {
        this.serviceToBlockingMethods.putAll(serviceToBlockingMethods);
        this.hasBlockingMethods = true;
    }

    public static void propagateSecurityIdentityWithDuplicatedCtx(RoutingContext event) {
        Context context = getCapturedVertxContext();
        if (context != null) {
            if (event.user() instanceof QuarkusHttpUser existing) {
                getCapturedVertxContext().putLocal(IDENTITY_KEY, existing.getSecurityIdentity());
            } else {
                getCapturedVertxContext().putLocal(DEFERRED_IDENTITY_KEY, QuarkusHttpUser.getSecurityIdentity(event, null));
            }
        }
    }

    private static Context getCapturedVertxContext() {
        // this is only running when gRPC is run as Vert.x HTTP route handler, therefore we should be on duplicated context
        Context capturedVertxContext = Vertx.currentContext();
        if (capturedVertxContext == null || !isDuplicatedContext(capturedVertxContext)
                || isExplicitlyMarkedAsUnsafe(capturedVertxContext)) {
            log.warn("Unable to prepare request authentication - authentication must run on Vert.x duplicated context");
            return null;
        }
        return capturedVertxContext;
    }
}
