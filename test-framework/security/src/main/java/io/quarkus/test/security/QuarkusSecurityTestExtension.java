package io.quarkus.test.security;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;

import javax.enterprise.inject.spi.CDI;

import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.test.junit.callback.QuarkusTestAfterEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestBeforeEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;

public class QuarkusSecurityTestExtension implements QuarkusTestBeforeEachCallback, QuarkusTestAfterEachCallback {

    @Override
    public void afterEach(QuarkusTestMethodContext context) {
        CDI.current().select(TestAuthController.class).get().setEnabled(true);
        CDI.current().select(TestIdentityAssociation.class).get().setTestIdentity(null);
    }

    @Override
    public void beforeEach(QuarkusTestMethodContext context) {
        try {
            //the usual ClassLoader hacks to get our copy of the TestSecurity annotation
            ClassLoader cl = QuarkusSecurityTestExtension.class.getClassLoader();
            Class<?> original = cl.loadClass(context.getTestMethod().getDeclaringClass().getName());
            Method method = original.getDeclaredMethod(context.getTestMethod().getName(),
                    Arrays.stream(context.getTestMethod().getParameterTypes()).map(s -> {
                        if (s.isPrimitive()) {
                            return s;
                        }
                        try {
                            return Class.forName(s.getName(), false, cl);
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    }).toArray(Class<?>[]::new));
            TestSecurity testSecurity = method.getAnnotation(TestSecurity.class);
            if (testSecurity == null) {
                testSecurity = original.getAnnotation(TestSecurity.class);
                while (testSecurity == null && original != Object.class) {
                    original = original.getSuperclass();
                    testSecurity = original.getAnnotation(TestSecurity.class);
                }
            }
            if (testSecurity == null) {
                return;
            }
            CDI.current().select(TestAuthController.class).get().setEnabled(testSecurity.authorizationEnabled());
            if (testSecurity.user().isEmpty()) {
                if (testSecurity.roles().length != 0) {
                    throw new RuntimeException("Cannot specify roles without a username in @TestSecurity");
                }
            } else {
                QuarkusSecurityIdentity user = QuarkusSecurityIdentity.builder()
                        .setPrincipal(new QuarkusPrincipal(testSecurity.user()))
                        .addRoles(new HashSet<>(Arrays.asList(testSecurity.roles()))).build();
                CDI.current().select(TestIdentityAssociation.class).get().setTestIdentity(user);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to setup @TestSecurity", e);
        }

    }
}
