package io.quarkus.test.security.common;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.test.junit.callback.QuarkusTestAfterEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestBeforeEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;

public abstract class AbstractSecurityTestExtension implements QuarkusTestBeforeEachCallback, QuarkusTestAfterEachCallback {

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

            TestSecurityProperties testSecurity = getTestSecurityProperties(original, method);
            if (testSecurity == null) {
                return;
            }

            CDI.current().select(TestAuthController.class).get().setEnabled(testSecurity.isAuthorizationEnabled());
            if (testSecurity.getUser().isEmpty()) {
                if (testSecurity.getRoles().length != 0) {
                    throw new RuntimeException("Cannot specify roles without a username in @TestSecurity");
                }
            } else {
                QuarkusSecurityIdentity.Builder user = QuarkusSecurityIdentity.builder()
                        .setPrincipal(new QuarkusPrincipal(testSecurity.getUser()))
                        .addRoles(new HashSet<>(Arrays.asList(testSecurity.getRoles())));

                if (testSecurity.getAttributes() != null) {
                    user.addAttributes(Arrays.stream(testSecurity.getAttributes())
                            .collect(Collectors.toMap(s -> s.key(), s -> s.value())));
                }
                if (testSecurity.getExtraProperties() != null) {
                    for (Map.Entry<String, String> entry : testSecurity.getExtraProperties().entrySet()) {
                        user.addAttribute(entry.getKey(), entry.getValue());
                    }
                }

                SecurityIdentity userIdentity = augment(user.build());
                CDI.current().select(TestIdentityAssociation.class).get().setTestIdentity(userIdentity);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to setup TestSecurity", e);
        }

    }

    private SecurityIdentity augment(SecurityIdentity identity) {
        Instance<TestSecurityIdentityAugmentor> producer = CDI.current().select(TestSecurityIdentityAugmentor.class);
        if (producer.isResolvable()) {
            return producer.get().augment(identity);
        }
        return identity;
    }

    @SuppressWarnings("unchecked")
    protected <T> T getTestSecurityAnnotation(Class<? extends Annotation> annotationClass, Class<?> original, Method method) {
        Annotation testSecurity = method.getAnnotation(annotationClass);
        if (testSecurity == null) {
            testSecurity = original.getAnnotation(annotationClass);
            while (testSecurity == null && original != Object.class) {
                original = original.getSuperclass();
                testSecurity = original.getAnnotation(annotationClass);
            }
        }
        return (T) testSecurity;
    }

    protected abstract TestSecurityProperties getTestSecurityProperties(Class<?> original, Method method);
}
