package io.quarkus.security.spi.runtime;

import java.lang.reflect.Method;

public interface SecurityCheckStorage {

    default SecurityCheck getSecurityCheck(Method method) {
        return getSecurityCheck(MethodDescription.ofMethod(method));
    }

    SecurityCheck getSecurityCheck(MethodDescription methodDescription);

}
