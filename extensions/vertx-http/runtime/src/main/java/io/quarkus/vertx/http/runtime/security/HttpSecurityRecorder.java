package io.quarkus.vertx.http.runtime.security;

import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import javax.enterprise.inject.spi.CDI;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class HttpSecurityRecorder {

    public Handler<RoutingContext> authenticationMechanismHandler() {
        return new Handler<RoutingContext>() {

            volatile HttpAuthenticator authenticator;

            @Override
            public void handle(RoutingContext event) {
                if (authenticator == null) {
                    authenticator = CDI.current().select(HttpAuthenticator.class).get();
                }
                //we put the authenticator into the routing context so it can be used by other systems
                event.put(HttpAuthenticator.class.getName(), authenticator);
                authenticator.attemptAuthentication(event).handle(new BiFunction<SecurityIdentity, Throwable, Object>() {
                    @Override
                    public Object apply(SecurityIdentity identity, Throwable throwable) {
                        if (throwable != null) {
                            while (throwable instanceof CompletionException && throwable.getCause() != null) {
                                throwable = throwable.getCause();
                            }
                            //auth failed
                            if (throwable instanceof AuthenticationFailedException) {
                                authenticator.sendChallenge(event, new Runnable() {
                                    @Override
                                    public void run() {
                                        event.response().end();
                                    }
                                });
                            } else if (throwable instanceof AuthenticationRedirectException) {
                                AuthenticationRedirectException redirectEx = (AuthenticationRedirectException) throwable;
                                event.response().setStatusCode(redirectEx.getCode());
                                event.response().headers().set(HttpHeaders.LOCATION, redirectEx.getRedirectUri());
                                event.response().end();
                            } else {
                                event.fail(throwable);
                            }
                            return null;
                        }
                        if (event.response().ended()) {
                            return null;
                        }
                        if (identity != null) {
                            event.setUser(new QuarkusHttpUser(identity));
                        }
                        event.next();
                        return null;
                    }
                });
            }
        };
    }

    public Handler<RoutingContext> permissionCheckHandler() {
        return new Handler<RoutingContext>() {
            volatile HttpAuthorizer authorizer;

            @Override
            public void handle(RoutingContext event) {
                if (authorizer == null) {
                    authorizer = CDI.current().select(HttpAuthorizer.class).get();
                }
                authorizer.checkPermission(event);
            }
        };
    }

    public BeanContainerListener initPermissions(HttpBuildTimeConfig permissions,
            Map<String, Supplier<HttpSecurityPolicy>> policies) {
        return new BeanContainerListener() {
            @Override
            public void created(BeanContainer container) {
                container.instance(PathMatchingHttpSecurityPolicy.class).init(permissions, policies);
            }
        };
    }

    public void setupFormAuth(BeanContainer container, HttpConfiguration httpConfiguration,
            HttpBuildTimeConfig buildTimeConfig) {
        container.instance(FormAuthenticationMechanism.class).init(httpConfiguration, buildTimeConfig);
    }
}
