package io.quarkus.security.runtime.interceptor;

import java.util.Map;

import io.quarkus.security.spi.runtime.MethodDescription;
import io.quarkus.security.spi.runtime.SecurityCheck;
import io.quarkus.security.spi.runtime.SecurityCheckStorage;

public final class SecurityCheckStorageImpl implements SecurityCheckStorage {

    private final Map<MethodDescription, SecurityCheck> securityChecks;
    private final SecurityCheck defaultSecurityCheck;

    public SecurityCheckStorageImpl(Map<MethodDescription, SecurityCheck> securityChecks, SecurityCheck defaultSecurityCheck) {
        this.securityChecks = Map.copyOf(securityChecks);
        this.defaultSecurityCheck = defaultSecurityCheck;
    }

    @Override
    public SecurityCheck getSecurityCheck(MethodDescription methodDescription) {
        return securityChecks.get(methodDescription);
    }

    @Override
    public SecurityCheck getDefaultSecurityCheck() {
        return defaultSecurityCheck;
    }
}
