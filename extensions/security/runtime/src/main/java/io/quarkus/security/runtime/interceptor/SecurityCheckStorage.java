package io.quarkus.security.runtime.interceptor;

import java.lang.reflect.Method;

import io.quarkus.security.runtime.interceptor.check.SecurityCheck;

public interface SecurityCheckStorage {
    SecurityCheck getSecurityCheck(Method method);
}
