package io.quarkus.spring.security.runtime.interceptor.check;

import java.util.List;

/**
 * Build-time description of the {@link io.quarkus.security.spi.runtime.SecurityCheck} that secures a method annotated
 * with the Spring Security annotations.
 */
public sealed interface SpringSecurityCheckSpec {

    enum Standard implements SpringSecurityCheckSpec {
        PERMIT_ALL,
        DENY_ALL,
        AUTHENTICATED,
        ANONYMOUS
    }

    record RolesAllowed(List<String> roles) implements SpringSecurityCheckSpec {

        public RolesAllowed {
            roles = List.copyOf(roles);
        }
    }

    record HasAnyRole(List<RoleSupplier> roleSuppliers) implements SpringSecurityCheckSpec {

        public HasAnyRole {
            roleSuppliers = List.copyOf(roleSuppliers);
        }
    }

    sealed interface RoleSupplier {
    }

    record StaticRole(String role) implements RoleSupplier {
    }

    record BeanFieldRole(String beanClass, String fieldName) implements RoleSupplier {
    }

    record All(List<SpringSecurityCheckSpec> checks) implements SpringSecurityCheckSpec {

        public All {
            checks = List.copyOf(checks);
        }
    }

    record Any(List<SpringSecurityCheckSpec> checks) implements SpringSecurityCheckSpec {

        public Any {
            checks = List.copyOf(checks);
        }
    }

    record PrincipalNameFromParameter(int parameterIndex,
            PrincipalNameFromParameterSecurityCheck.CheckType checkType) implements SpringSecurityCheckSpec {
    }

    record PrincipalNameFromParameterObject(int parameterIndex, String expectedParameterClass,
            String stringPropertyAccessorClass, String propertyName,
            PrincipalNameFromParameterObjectSecurityCheck.CheckType checkType) implements SpringSecurityCheckSpec {
    }

    record GeneratedClass(String className) implements SpringSecurityCheckSpec {
    }
}
