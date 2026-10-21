package io.quarkus.spring.security.runtime.interceptor.check;

import java.util.List;

/**
 * Build-time description of a method secured with the Spring Security annotations.
 *
 * @param className declaring class name
 * @param methodName method name
 * @param parameterTypes method parameter type names
 * @param check the security check description
 */
public record SpringSecuredMethod(String className, String methodName, List<String> parameterTypes,
        SpringSecurityCheckSpec check) {

    public SpringSecuredMethod {
        parameterTypes = List.copyOf(parameterTypes);
    }
}
