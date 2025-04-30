package io.quarkus.security.spi.runtime;

import java.lang.reflect.Method;

import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;

public interface SecurityCheck {

    void apply(SecurityIdentity identity, Method method, Object[] parameters);

    default Uni<?> nonBlockingApply(SecurityIdentity identity, Method method, Object[] parameters) {
        try {
            apply(identity, method, parameters);
        } catch (Exception e) {
            return Uni.createFrom().failure(e);
        }
        return Uni.createFrom().nullItem();
    }

    void apply(SecurityIdentity identity, MethodDescription methodDescription, Object[] parameters);

    default Uni<?> nonBlockingApply(SecurityIdentity identity, MethodDescription methodDescription, Object[] parameters) {
        try {
            apply(identity, methodDescription, parameters);
        } catch (Exception e) {
            return Uni.createFrom().failure(e);
        }
        return Uni.createFrom().nullItem();
    }

    default boolean isPermitAll() {
        return false;
    }

    /**
     * Security checks may be performed before the secured method is actually invoked.
     * This happens to make sure they are run before serialization and fully asynchronous checks work as expected.
     * However, if the security checks requires arguments of invoked method, it is possible to postpone this check
     * to the moment when arguments are available.
     *
     * IMPORTANT: in order to avoid security risks, all requests with postponed security checks must be authenticated
     *
     * @return true if the security check needs method parameters to work correctly
     */
    default boolean requiresMethodArguments() {
        return false;
    }
}
