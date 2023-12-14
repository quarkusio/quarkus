package io.quarkus.security.runtime.interceptor;

import static io.quarkus.security.spi.runtime.SecurityEventHelper.AUTHORIZATION_FAILURE;
import static io.quarkus.security.spi.runtime.SecurityEventHelper.AUTHORIZATION_SUCCESS;

import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkus.runtime.BlockingOperationNotAllowedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.SecurityConfig;
import io.quarkus.security.runtime.SecurityIdentityAssociation;
import io.quarkus.security.spi.runtime.AuthorizationFailureEvent;
import io.quarkus.security.spi.runtime.AuthorizationSuccessEvent;
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

    @Inject
    SecurityIdentityAssociation identityAssociation;

    SecurityConstrainer(SecurityCheckStorage storage, BeanManager beanManager, SecurityConfig securityConfig,
            Event<AuthorizationFailureEvent> authZFailureEvent, Event<AuthorizationSuccessEvent> authZSuccessEvent) {
        this.storage = storage;
        this.securityEventHelper = new SecurityEventHelper<>(authZSuccessEvent, authZFailureEvent, AUTHORIZATION_SUCCESS,
                AUTHORIZATION_FAILURE, beanManager, securityConfig.events().enabled());
    }

    public void check(Method method, Object[] parameters) {
        SecurityCheck securityCheck = storage.getSecurityCheck(method);
        SecurityIdentity identity = null;
        if (securityCheck != null && !securityCheck.isPermitAll()) {
            try {
                identity = identityAssociation.getIdentity();
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
                    fireAuthZFailureEvent(identity, exception, securityCheck);
                    throw exception;
                }
            } else {
                securityCheck.apply(identity, method, parameters);
            }
        }
        if (securityEventHelper.fireEventOnSuccess()) {
            fireAuthZSuccessEvent(securityCheck, identity);
        }
    }

    public Uni<?> nonBlockingCheck(Method method, Object[] parameters) {
        SecurityCheck securityCheck = storage.getSecurityCheck(method);
        if (securityCheck != null) {
            if (!securityCheck.isPermitAll()) {
                return identityAssociation.getDeferredIdentity()
                        .onItem()
                        .transformToUni(new Function<SecurityIdentity, Uni<?>>() {
                            @Override
                            public Uni<?> apply(SecurityIdentity securityIdentity) {
                                Uni<?> checkResult = securityCheck.nonBlockingApply(securityIdentity, method, parameters);
                                if (securityEventHelper.fireEventOnFailure()) {
                                    checkResult = checkResult.onFailure().invoke(new Consumer<Throwable>() {
                                        @Override
                                        public void accept(Throwable throwable) {
                                            fireAuthZFailureEvent(securityIdentity, throwable, securityCheck);
                                        }
                                    });
                                }
                                if (securityEventHelper.fireEventOnSuccess()) {
                                    checkResult = checkResult.invoke(new Runnable() {
                                        @Override
                                        public void run() {
                                            fireAuthZSuccessEvent(securityCheck, securityIdentity);
                                        }
                                    });
                                }
                                return checkResult;
                            }
                        });
            } else if (securityEventHelper.fireEventOnSuccess()) {
                fireAuthZSuccessEvent(securityCheck, null);
            }
        }
        return Uni.createFrom().item(CHECK_OK);
    }

    private void fireAuthZSuccessEvent(SecurityCheck securityCheck, SecurityIdentity identity) {
        var securityCheckName = securityCheck == null ? null : securityCheck.getClass().getName();
        securityEventHelper.fireSuccessEvent(new AuthorizationSuccessEvent(identity, securityCheckName, null));
    }

    private void fireAuthZFailureEvent(SecurityIdentity identity, Throwable failure, SecurityCheck securityCheck) {
        securityEventHelper
                .fireFailureEvent(new AuthorizationFailureEvent(identity, failure, securityCheck.getClass().getName()));
    }
}
