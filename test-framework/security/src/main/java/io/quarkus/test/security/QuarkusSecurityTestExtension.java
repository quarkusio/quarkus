package io.quarkus.test.security;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;

import io.quarkus.security.identity.SecurityIdentity;
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
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
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
            Annotation[] allAnnotations = new Annotation[] {};
            TestSecurity testSecurity = method.getAnnotation(TestSecurity.class);
            if (testSecurity == null) {
                testSecurity = original.getAnnotation(TestSecurity.class);
                if (testSecurity != null) {
                    allAnnotations = original.getAnnotations();
                }
                while (testSecurity == null && original != Object.class) {
                    original = original.getSuperclass();
                    testSecurity = original.getAnnotation(TestSecurity.class);
                    if (testSecurity != null) {
                        allAnnotations = original.getAnnotations();
                    }
                }
            } else {
                allAnnotations = method.getAnnotations();
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
                QuarkusSecurityIdentity.Builder user = QuarkusSecurityIdentity.builder()
                        .setPrincipal(new QuarkusPrincipal(testSecurity.user()))
                        .addRoles(new HashSet<>(Arrays.asList(testSecurity.roles())));

                if (testSecurity.attributes() != null) {
                    user.addAttributes(Arrays.stream(testSecurity.attributes())
                            .collect(Collectors.toMap(s -> s.key(), s -> s.value())));
                }

                SecurityIdentity userIdentity = augment(user.build(), allAnnotations);
                CDI.current().select(TestIdentityAssociation.class).get().setTestIdentity(userIdentity);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to setup @TestSecurity", e);
        }

    }

    private SecurityIdentity augment(SecurityIdentity identity, Annotation[] annotations) {
        Instance<TestSecurityIdentityAugmentor> producer = CDI.current().select(TestSecurityIdentityAugmentor.class);
        if (producer.isResolvable()) {
            return producer.get().augment(identity, annotations);
        }
        return identity;
    }
}
