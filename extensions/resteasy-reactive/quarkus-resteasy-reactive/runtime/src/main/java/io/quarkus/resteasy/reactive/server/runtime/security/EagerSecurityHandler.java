package io.quarkus.resteasy.reactive.server.runtime.security;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.model.ServerResourceMethod;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.SecurityCheck;
import io.quarkus.security.spi.runtime.SecurityCheckStorage;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class EagerSecurityHandler implements ServerRestHandler {

    private static final SecurityCheck NULL_SENTINEL = new SecurityCheck() {
        @Override
        public void apply(SecurityIdentity identity, Method method, Object[] parameters) {

        }
    };

    private volatile InjectableInstance<CurrentIdentityAssociation> currentIdentityAssociation;
    private volatile SecurityCheck check;

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        SecurityCheck check = this.check;
        if (check == null) {
            check = Arc.container().instance(SecurityCheckStorage.class).get()
                    .getSecurityCheck(requestContext.getTarget().getLazyMethod().getMethod());
            if (check == null) {
                check = NULL_SENTINEL;
            }
            this.check = check;
        }
        if (check == NULL_SENTINEL) {
            return;
        }
        requestContext.requireCDIRequestScope();
        requestContext.suspend();
        SecurityCheck theCheck = check;
        getCurrentIdentityAssociation().get().getDeferredIdentity().map(new Function<SecurityIdentity, Object>() {
            @Override
            public Object apply(SecurityIdentity securityIdentity) {
                theCheck.apply(securityIdentity, requestContext.getTarget().getLazyMethod().getMethod(),
                        requestContext.getParameters());
                return null;
            }
        })
                .subscribe().withSubscriber(new UniSubscriber<Object>() {
                    @Override
                    public void onSubscribe(UniSubscription subscription) {

                    }

                    @Override
                    public void onItem(Object item) {
                        requestContext.resume();
                    }

                    @Override
                    public void onFailure(Throwable failure) {
                        requestContext.resume(failure);
                    }
                });

    }

    private InjectableInstance<CurrentIdentityAssociation> getCurrentIdentityAssociation() {
        InjectableInstance<CurrentIdentityAssociation> identityAssociation = this.currentIdentityAssociation;
        if (identityAssociation == null) {
            return this.currentIdentityAssociation = Arc.container().select(CurrentIdentityAssociation.class);
        }
        return identityAssociation;
    }

    public static class Customizer implements HandlerChainCustomizer {
        @Override
        public List<ServerRestHandler> handlers(Phase phase, ResourceClass resourceClass,
                ServerResourceMethod serverResourceMethod) {
            if (phase == Phase.AFTER_MATCH) {
                return Collections.singletonList(new EagerSecurityHandler());
            }
            return Collections.emptyList();
        }
    }
}
