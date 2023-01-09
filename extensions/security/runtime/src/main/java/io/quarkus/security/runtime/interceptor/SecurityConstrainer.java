package io.quarkus.security.runtime.interceptor;

import java.lang.reflect.Method;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkus.runtime.BlockingOperationNotAllowedException;
import io.quarkus.security.runtime.SecurityIdentityAssociation;
import io.quarkus.security.spi.runtime.SecurityCheck;
import io.quarkus.security.spi.runtime.SecurityCheckStorage;
import io.smallrye.mutiny.Uni;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@Singleton
public class SecurityConstrainer {

    public static final Object CHECK_OK = new Object();
    @Inject
    SecurityIdentityAssociation identity;

    @Inject
    SecurityCheckStorage storage;

    public void check(Method method, Object[] parameters) {

        SecurityCheck securityCheck = storage.getSecurityCheck(method);
        if (securityCheck != null && !securityCheck.isPermitAll()) {
            try {
                securityCheck.apply(identity.getIdentity(), method, parameters);
            } catch (BlockingOperationNotAllowedException blockingException) {
                throw new BlockingOperationNotAllowedException(
                        "Blocking security check attempted in code running on the event loop. " +
                                "Make the secured method return an async type, i.e. Uni, Multi or CompletionStage, or " +
                                "use an authentication mechanism that sets the SecurityIdentity in a blocking manner " +
                                "prior to delegating the call",
                        blockingException);
            }
        }
    }

    public Uni<?> nonBlockingCheck(Method method, Object[] parameters) {
        SecurityCheck securityCheck = storage.getSecurityCheck(method);
        if (securityCheck != null && !securityCheck.isPermitAll()) {
            return identity.getDeferredIdentity()
                    .onItem()
                    .invoke(identity -> securityCheck.apply(identity, method, parameters));
        }
        return Uni.createFrom().item(CHECK_OK);
    }
}
