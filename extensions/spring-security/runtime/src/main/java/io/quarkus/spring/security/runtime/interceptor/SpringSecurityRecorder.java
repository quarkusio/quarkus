package io.quarkus.spring.security.runtime.interceptor;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Supplier;

import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.security.runtime.interceptor.check.SupplierRolesAllowedCheck;
import io.quarkus.security.spi.runtime.SecurityCheck;
import io.quarkus.spring.security.runtime.interceptor.check.AllDelegatingSecurityCheck;
import io.quarkus.spring.security.runtime.interceptor.check.AnonymousCheck;
import io.quarkus.spring.security.runtime.interceptor.check.AnyDelegatingSecurityCheck;
import io.quarkus.spring.security.runtime.interceptor.check.CombinedRoleSupplier;
import io.quarkus.spring.security.runtime.interceptor.check.FromBeanRoleSupplier;
import io.quarkus.spring.security.runtime.interceptor.check.PrincipalNameFromParameterObjectSecurityCheck;
import io.quarkus.spring.security.runtime.interceptor.check.PrincipalNameFromParameterSecurityCheck;

@Recorder
public class SpringSecurityRecorder {

    public SecurityCheck anonymous() {
        return AnonymousCheck.INSTANCE;
    }

    public SecurityCheck allDelegating(List<SecurityCheck> securityChecks) {
        return new AllDelegatingSecurityCheck(securityChecks);
    }

    public SecurityCheck anyDelegating(List<SecurityCheck> securityChecks) {
        return new AnyDelegatingSecurityCheck(securityChecks);
    }

    public Supplier<String[]> staticHasRole(String role) {
        return new Supplier<String[]>() {
            @Override
            public String[] get() {
                return new String[] { role };
            }
        };
    }

    public Supplier<String[]> fromBeanField(String className, String fieldName) {
        try {
            return new FromBeanRoleSupplier(Class.forName(className, false, Thread.currentThread().getContextClassLoader()),
                    fieldName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public SecurityCheck rolesAllowed(List<Supplier<String[]>> delegates) {
        return new SupplierRolesAllowedCheck(new CombinedRoleSupplier(delegates));
    }

    public SecurityCheck principalNameFromParameterSecurityCheck(int index,
            PrincipalNameFromParameterSecurityCheck.CheckType type) {
        return PrincipalNameFromParameterSecurityCheck.of(index, type);
    }

    public SecurityCheck fromGeneratedClass(String generatedClassName) {
        try {
            Class<?> type = Class.forName(generatedClassName, false, Thread.currentThread().getContextClassLoader());
            //invoked at static init, no reflection registration required
            Method m = type.getDeclaredMethod("getInstance");
            return (SecurityCheck) m.invoke(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public SecurityCheck principalNameFromParameterObjectSecurityCheck(int index, String expectedParameterClass,
            String stringPropertyAccessorClass, String propertyName) {
        return PrincipalNameFromParameterObjectSecurityCheck.of(index, expectedParameterClass, stringPropertyAccessorClass,
                propertyName);
    }
}
