package io.quarkus.grpc.auth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Prioritized;
import javax.inject.Inject;
import javax.inject.Singleton;

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
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

/**
 * Security interceptor invoking {@link GrpcSecurityMechanism} implementations
 */
@GlobalInterceptor
@Singleton
public final class GrpcSecurityInterceptor implements ServerInterceptor, Prioritized {

    private static final Logger log = Logger.getLogger(GrpcSecurityInterceptor.class);

    private final IdentityProviderManager identityProviderManager;
    private final CurrentIdentityAssociation identityAssociation;

    private final AuthExceptionHandlerProvider exceptionHandlerProvider;
    private final List<GrpcSecurityMechanism> securityMechanisms;

    @Inject
    public GrpcSecurityInterceptor(
            CurrentIdentityAssociation identityAssociation,
            IdentityProviderManager identityProviderManager,
            Instance<GrpcSecurityMechanism> securityMechanisms,
            Instance<AuthExceptionHandlerProvider> exceptionHandlers) {
        this.identityAssociation = identityAssociation;
        this.identityProviderManager = identityProviderManager;

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
        mechanisms.sort(Comparator.comparing(GrpcSecurityMechanism::getPriority));
        this.securityMechanisms = mechanisms;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall,
            Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {
        Exception error = null;
        for (GrpcSecurityMechanism securityMechanism : securityMechanisms) {
            if (securityMechanism.handles(metadata)) {
                try {
                    AuthenticationRequest authenticationRequest = securityMechanism.createAuthenticationRequest(metadata);
                    Context context = Vertx.currentContext();
                    boolean onEventLoopThread = Context.isOnEventLoopThread();

                    if (authenticationRequest != null) {
                        Uni<SecurityIdentity> auth = identityProviderManager
                                .authenticate(authenticationRequest)
                                .emitOn(new Executor() {
                                    @Override
                                    public void execute(Runnable command) {
                                        if (onEventLoopThread) {
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
        ServerCall.Listener<ReqT> listener = serverCallHandler.startCall(serverCall, metadata);
        return exceptionHandlerProvider.createHandler(listener, serverCall, metadata);
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE - 100;
    }
}
