package io.quarkus.oidc.runtime.dev.ui;

import java.time.Duration;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkus.oidc.SecurityEvent;
import io.quarkus.oidc.runtime.OidcConfig;
import io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class OidcDevLoginObserver {

    private final BroadcastProcessor<Boolean> oidcLoginStream;

    OidcDevLoginObserver(OidcConfig config) {
        boolean isWebApplication = ApplicationType.WEB_APP == OidcConfig.getDefaultTenant(config).applicationType()
                .orElse(null);
        if (isWebApplication) {
            this.oidcLoginStream = BroadcastProcessor.create();
        } else {
            this.oidcLoginStream = null;
        }
    }

    void observeOidcLogin(@Observes SecurityEvent event) {
        if (oidcLoginStream != null && event.getEventType() == SecurityEvent.Type.OIDC_LOGIN) {
            RoutingContext routingContext = event.getSecurityIdentity().getAttribute(RoutingContext.class.getName());
            if (routingContext != null && !routingContext.response().ended()) {
                routingContext.addEndHandler(new Handler<AsyncResult<Void>>() {
                    @Override
                    public void handle(AsyncResult<Void> voidAsyncResult) {
                        oidcLoginStream.onNext(true);
                    }
                });
            } else {
                oidcLoginStream.onNext(true);
            }
        }
    }

    Multi<Boolean> streamOidcLoginEvent() {
        return oidcLoginStream == null ? Multi.createFrom().empty() : oidcLoginStream.onItem().call(delayByOneSecond());
    }

    private static Function<Boolean, Uni<?>> delayByOneSecond() {
        return new Function<Boolean, Uni<?>>() {
            @Override
            public Uni<?> apply(Boolean i) {
                if (Boolean.TRUE.equals(i)) {
                    // we inform about login once response has ended,
                    // but we need to wait a bit till response is sent and cookies present on the browser side
                    // if this proves unreliable, we can add retry on the front end side instead of the delay
                    return Uni.createFrom().item(true).onItem().delayIt().by(Duration.ofSeconds(1));
                } else {
                    return Uni.createFrom().nothing();
                }
            }
        };
    }

}
