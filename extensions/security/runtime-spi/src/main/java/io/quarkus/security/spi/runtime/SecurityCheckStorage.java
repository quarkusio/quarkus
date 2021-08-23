package io.quarkus.security.spi.runtime;

import java.lang.reflect.Method;

public interface SecurityCheckStorage {
    SecurityCheck getSecurityCheck(Method method);

}
