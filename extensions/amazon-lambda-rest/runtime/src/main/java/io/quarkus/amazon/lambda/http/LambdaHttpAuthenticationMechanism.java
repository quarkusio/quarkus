package io.quarkus.amazon.lambda.http;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.quarkus.amazon.lambda.http.model.AwsProxyRequest;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.vertx.http.runtime.QuarkusHttpHeaders;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class LambdaHttpAuthenticationMechanism implements HttpAuthenticationMechanism {
    @Inject
    Instance<IdentityProvider<LambdaAuthenticationRequest>> identityProviders;

    // there is no way in CDI to currently provide a prioritized list of IdentityProvider
    // So, what we do here is to try to see if anybody has registered one.  If no identity, then
    // fire off a request that can only be resolved by the DefaultLambdaIdentityProvider
    boolean useDefault;

    @PostConstruct
    public void initialize() {
        useDefault = !identityProviders.iterator().hasNext();
    }

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext routingContext, IdentityProviderManager identityProviderManager) {
        MultiMap qheaders = routingContext.request().headers();
        if (qheaders instanceof QuarkusHttpHeaders) {
            Map<Class<?>, Object> contextObjects = ((QuarkusHttpHeaders) qheaders).getContextObjects();
            if (contextObjects.containsKey(AwsProxyRequest.class)) {
                AwsProxyRequest event = (AwsProxyRequest) contextObjects.get(AwsProxyRequest.class);
                if (isAuthenticatable(event)) {
                    if (useDefault) {
                        return identityProviderManager
                                .authenticate(HttpSecurityUtils.setRoutingContextAttribute(
                                        new DefaultLambdaAuthenticationRequest(event), routingContext));

                    } else {
                        return identityProviderManager
                                .authenticate(HttpSecurityUtils.setRoutingContextAttribute(
                                        new LambdaAuthenticationRequest(event), routingContext));
                    }
                }
            }
        }
        return Uni.createFrom().optional(Optional.empty());
    }

    private boolean isAuthenticatable(AwsProxyRequest event) {
        final Map<String, String> systemEnvironment = System.getenv();
        final boolean isSamLocal = Boolean.parseBoolean(systemEnvironment.get("AWS_SAM_LOCAL"));
        final String forcedUserName = systemEnvironment.get("QUARKUS_AWS_LAMBDA_FORCE_USER_NAME");
        return (isSamLocal && forcedUserName != null) || (event.getRequestContext() != null
                && (event.getRequestContext().getAuthorizer() != null || event.getRequestContext().getIdentity() != null));
    }

    @Override
    public Uni<Boolean> sendChallenge(RoutingContext context) {
        return Uni.createFrom().item(false);
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        return Uni.createFrom().nullItem();
    }

    static final Set<Class<? extends AuthenticationRequest>> credentialTypes = new HashSet<>();

    static {
        credentialTypes.add(LambdaAuthenticationRequest.class);
        credentialTypes.add(DefaultLambdaAuthenticationRequest.class);
    }

    @Override
    public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
        return credentialTypes;
    }

    @Override
    public HttpCredentialTransport getCredentialTransport() {
        return null;
    }
}
