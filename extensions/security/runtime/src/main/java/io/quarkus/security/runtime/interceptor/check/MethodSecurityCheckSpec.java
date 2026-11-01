package io.quarkus.security.runtime.interceptor.check;

import java.util.List;

/**
 * Build-time description of the {@link io.quarkus.security.spi.runtime.SecurityCheck} that secures one method.
 *
 * @param className declaring class name
 * @param methodName method name
 * @param parameterTypes method parameter type names
 * @param check the security check description
 */
public record MethodSecurityCheckSpec(String className, String methodName, List<String> parameterTypes,
        SecurityCheckSpec check) {

    public MethodSecurityCheckSpec {
        parameterTypes = List.copyOf(parameterTypes);
    }
}
