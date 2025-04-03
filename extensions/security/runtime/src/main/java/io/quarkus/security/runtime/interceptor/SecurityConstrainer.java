package io.quarkus.security.runtime.interceptor;

import static io.quarkus.security.spi.runtime.SecurityEventHelper.AUTHORIZATION_FAILURE;
import static io.quarkus.security.spi.runtime.SecurityEventHelper.AUTHORIZATION_SUCCESS;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.runtime.BlockingOperationNotAllowedException;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.AuthorizationFailureEvent;
import io.quarkus.security.spi.runtime.AuthorizationSuccessEvent;
import io.quarkus.security.spi.runtime.MethodDescription;
import io.quarkus.security.spi.runtime.SecurityCheck;
import io.quarkus.security.spi.runtime.SecurityCheckStorage;
import io.quarkus.security.spi.runtime.SecurityEventHelper;
import io.smallrye.mutiny.Uni;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@Singleton
public class SecurityConstrainer {

    public static final Object CHECK_OK = new Object();
    private final SecurityCheckStorage storage;
    private final SecurityEventHelper<AuthorizationSuccessEvent, AuthorizationFailureEvent> securityEventHelper;
    private final Instance<CurrentIdentityAssociation> securityIdentityAssociation;
    private final Supplier<Map<String, Object>> additionalEventPropsSupplier;

    public SecurityConstrainer(SecurityCheckStorage storage, BeanManager beanManager,
            Event<AuthorizationFailureEvent> authZFailureEvent, Event<AuthorizationSuccessEvent> authZSuccessEvent,
            boolean runtimeConfigReady, Instance<CurrentIdentityAssociation> securityIdentityAssociation,
            Supplier<Map<String, Object>> additionalEventPropsSupplier) {
        this.securityIdentityAssociation = securityIdentityAssociation;
        this.additionalEventPropsSupplier = additionalEventPropsSupplier;
        this.storage = storage;
        if (runtimeConfigReady) {
            boolean securityEventsEnabled = ConfigProvider.getConfig().getValue("quarkus.security.events.enabled",
                    Boolean.class);
            this.securityEventHelper = new SecurityEventHelper<>(authZSuccessEvent, authZFailureEvent, AUTHORIZATION_SUCCESS,
                    AUTHORIZATION_FAILURE, beanManager, securityEventsEnabled);
        } else {
            // static interceptors are initialized during the static init, therefore we need to initialize the helper lazily
            this.securityEventHelper = SecurityEventHelper.lazilyOf(authZSuccessEvent, authZFailureEvent,
                    AUTHORIZATION_SUCCESS, AUTHORIZATION_FAILURE, beanManager);
        }
    }

    public void check(Method method, Object[] parameters) {
        SecurityCheck securityCheck = storage.getSecurityCheck(method);
        SecurityIdentity identity = null;
        if (securityCheck != null && !securityCheck.isPermitAll()) {
            try {
                identity = securityIdentityAssociation.get().getIdentity();
            } catch (BlockingOperationNotAllowedException blockingException) {
                throw new BlockingOperationNotAllowedException(
                        "Blocking security check attempted in code running on the event loop. " +
                                "Make the secured method return an async type, i.e. Uni, Multi or CompletionStage, or " +
                                "use an authentication mechanism that sets the SecurityIdentity in a blocking manner " +
                                "prior to delegating the call",
                        blockingException);
            }
            if (securityEventHelper.fireEventOnFailure()) {
                try {
                    securityCheck.apply(identity, method, parameters);
                } catch (Exception exception) {
                    fireAuthZFailureEvent(identity, exception, securityCheck, method);
                    throw exception;
                }
            } else {
                securityCheck.apply(identity, method, parameters);
            }
        }
        if (securityEventHelper.fireEventOnSuccess()) {
            fireAuthZSuccessEvent(securityCheck, identity, method);
        }
    }

    public Uni<?> nonBlockingCheck(Method method, Object[] parameters) {
        SecurityCheck securityCheck = storage.getSecurityCheck(method);
        if (securityCheck != null) {
            if (!securityCheck.isPermitAll()) {
                return securityIdentityAssociation.get().getDeferredIdentity()
                        .onItem()
                        .transformToUni(new Function<SecurityIdentity, Uni<?>>() {
                            @Override
                            public Uni<?> apply(SecurityIdentity securityIdentity) {
                                Uni<?> checkResult = securityCheck.nonBlockingApply(securityIdentity, method, parameters);
                                if (securityEventHelper.fireEventOnFailure()) {
                                    checkResult = checkResult.onFailure().invoke(new Consumer<Throwable>() {
                                        @Override
                                        public void accept(Throwable throwable) {
                                            fireAuthZFailureEvent(securityIdentity, throwable, securityCheck, method);
                                        }
                                    });
                                }
                                if (securityEventHelper.fireEventOnSuccess()) {
                                    checkResult = checkResult.invoke(new Runnable() {
                                        @Override
                                        public void run() {
                                            fireAuthZSuccessEvent(securityCheck, securityIdentity, method);
                                        }
                                    });
                                }
                                return checkResult;
                            }
                        });
            } else if (securityEventHelper.fireEventOnSuccess()) {
                fireAuthZSuccessEvent(securityCheck, null, method);
            }
        }
        return Uni.createFrom().item(CHECK_OK);
    }

    private void fireAuthZSuccessEvent(SecurityCheck securityCheck, SecurityIdentity identity, Method method) {
        var securityCheckName = securityCheck == null ? null : securityCheck.getClass().getName();
        var additionalEventProps = additionalEventPropsSupplier.get();
        if (identity == null) {
            // get identity from event if auth already finished
            identity = (SecurityIdentity) additionalEventProps.get(SecurityIdentity.class.getName());
        }
        securityEventHelper.fireSuccessEvent(
                new AuthorizationSuccessEvent(identity, securityCheckName, additionalEventPropsSupplier.get(),
                        MethodDescription.ofMethod(method)));
    }

    private void fireAuthZFailureEvent(SecurityIdentity identity, Throwable failure, SecurityCheck securityCheck,
            Method method) {
        securityEventHelper
                .fireFailureEvent(new AuthorizationFailureEvent(identity, failure, securityCheck.getClass().getName(),
                        additionalEventPropsSupplier.get(), MethodDescription.ofMethod(method)));
    }
}
