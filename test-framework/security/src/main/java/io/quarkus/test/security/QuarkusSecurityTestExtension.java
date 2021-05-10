package io.quarkus.test.security;

import java.lang.reflect.Method;

import io.quarkus.test.security.common.AbstractSecurityTestExtension;
import io.quarkus.test.security.common.TestSecurityProperties;

public class QuarkusSecurityTestExtension extends AbstractSecurityTestExtension {

    protected TestSecurityProperties getTestSecurityProperties(Class<?> original, Method method) {
        TestSecurity testSecurity = super.getTestSecurityAnnotation(TestSecurity.class, original, method);
        return testSecurity == null ? null
                : new TestSecurityProperties(
                        testSecurity.user(), testSecurity.roles(), testSecurity.attributes(),
                        testSecurity.authorizationEnabled());
    }
}
