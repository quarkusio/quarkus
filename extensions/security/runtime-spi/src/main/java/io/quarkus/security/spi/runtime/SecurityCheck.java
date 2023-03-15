package io.quarkus.security.spi.runtime;

import java.lang.reflect.Method;

import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;

public interface SecurityCheck {

    void apply(SecurityIdentity identity, Method method, Object[] parameters);

    default Uni<?> nonBlockingApply(SecurityIdentity identity, Method method, Object[] parameters) {
        apply(identity, method, parameters);
        return Uni.createFrom().nullItem();
    }

    void apply(SecurityIdentity identity, MethodDescription methodDescription, Object[] parameters);

    default Uni<?> nonBlockingApply(SecurityIdentity identity, MethodDescription methodDescription, Object[] parameters) {
        apply(identity, methodDescription, parameters);
        return Uni.createFrom().nullItem();
    }

    default boolean isPermitAll() {
        return false;
    }
}
