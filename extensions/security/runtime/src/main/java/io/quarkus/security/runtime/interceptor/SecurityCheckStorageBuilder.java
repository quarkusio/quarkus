package io.quarkus.security.runtime.interceptor;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.security.spi.runtime.MethodDescription;
import io.quarkus.security.spi.runtime.SecurityCheck;
import io.quarkus.security.spi.runtime.SecurityCheckStorage;

public class SecurityCheckStorageBuilder {
    private final Map<MethodDescription, SecurityCheck> securityChecks = new HashMap<>();
    private SecurityCheck defaultSecurityCheck;

    public void registerCheck(String className,
            String methodName,
            String[] parameterTypes,
            SecurityCheck securityCheck) {
        securityChecks.put(new MethodDescription(className, methodName, parameterTypes), securityCheck);
    }

    public void registerDefaultSecurityCheck(SecurityCheck defaultSecurityCheck) {
        if (this.defaultSecurityCheck != null) {
            throw new IllegalStateException("Default SecurityCheck has already been registered");
        }
        this.defaultSecurityCheck = defaultSecurityCheck;
    }

    public SecurityCheckStorage create() {
        return new SecurityCheckStorage() {
            @Override
            public SecurityCheck getSecurityCheck(MethodDescription methodDescription) {
                return securityChecks.get(methodDescription);
            }

            @Override
            public SecurityCheck getDefaultSecurityCheck() {
                return defaultSecurityCheck;
            }
        };
    }
}
