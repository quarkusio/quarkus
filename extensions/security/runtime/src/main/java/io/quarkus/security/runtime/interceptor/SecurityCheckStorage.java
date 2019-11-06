package io.quarkus.security.runtime.interceptor;

import java.lang.reflect.Method;
import java.util.function.Predicate;

import io.quarkus.security.runtime.interceptor.check.SecurityCheck;

public interface SecurityCheckStorage {
    SecurityCheck getSecurityCheck(Method method);

    class AppPredicate implements Predicate<String> {

        @Override
        public boolean test(String s) {
            return s.equals(SecurityCheckStorage.class.getName());
        }
    }
}
