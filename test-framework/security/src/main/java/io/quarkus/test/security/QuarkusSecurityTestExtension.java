package io.quarkus.test.security;

import static io.quarkus.security.PermissionsAllowed.PERMISSION_TO_ACTION_SEPARATOR;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.security.Permission;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;

import io.quarkus.security.StringPermission;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.test.junit.callback.QuarkusTestAfterEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestBeforeEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
import io.quarkus.test.util.annotations.AnnotationContainer;
import io.quarkus.test.util.annotations.AnnotationUtils;
import io.smallrye.mutiny.Uni;

public class QuarkusSecurityTestExtension implements QuarkusTestBeforeEachCallback, QuarkusTestAfterEachCallback {

    @Override
    public void afterEach(QuarkusTestMethodContext context) {
        try {
            if (getAnnotationContainer(context).isPresent()) {
                CDI.current().select(TestAuthController.class).get().setEnabled(true);
                for (var testMechanism : CDI.current().select(AbstractTestHttpAuthenticationMechanism.class)) {
                    testMechanism.setAuthMechanism(null);
                }
                var testIdentity = CDI.current().select(TestIdentityAssociation.class).get();
                testIdentity.setTestIdentity(null);
                testIdentity.setPathBasedIdentity(false);
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to reset TestAuthController, TestIdentityAssociation and TestHttpAuthenticationMechanism", e);
        }

    }

    @Override
    public void beforeEach(QuarkusTestMethodContext context) {
        try {
            Optional<AnnotationContainer<TestSecurity>> annotationContainerOptional = getAnnotationContainer(context);
            if (annotationContainerOptional.isEmpty()) {
                return;
            }
            var annotationContainer = annotationContainerOptional.get();
            Annotation[] allAnnotations = annotationContainer.getElement().getAnnotations();
            TestSecurity testSecurity = annotationContainer.getAnnotation();
            CDI.current().select(TestAuthController.class).get().setEnabled(testSecurity.authorizationEnabled());
            if (testSecurity.user().isEmpty()) {
                if (testSecurity.roles().length != 0) {
                    throw new RuntimeException("Cannot specify roles without a username in @TestSecurity");
                }
                if (testSecurity.permissions().length != 0) {
                    throw new RuntimeException("Cannot specify permissions without a username in @TestSecurity");
                }
            } else {
                QuarkusSecurityIdentity.Builder user = QuarkusSecurityIdentity.builder()
                        .setPrincipal(new QuarkusPrincipal(testSecurity.user()))
                        .addRoles(new HashSet<>(Arrays.asList(testSecurity.roles())));

                if (testSecurity.permissions().length != 0) {
                    user.addPermissionChecker(createPermissionChecker(testSecurity.permissions()));
                }

                if (testSecurity.attributes() != null) {
                    user.addAttributes(Arrays.stream(testSecurity.attributes())
                            .collect(Collectors.toMap(s -> s.key(), s -> s.type().convert(s.value()))));
                }

                SecurityIdentity userIdentity = augment(user.build(), allAnnotations);
                CDI.current().select(TestIdentityAssociation.class).get().setTestIdentity(userIdentity);
                if (!testSecurity.authMechanism().isEmpty()) {
                    for (var testMechanism : CDI.current().select(AbstractTestHttpAuthenticationMechanism.class)) {
                        testMechanism.setAuthMechanism(testSecurity.authMechanism());
                    }
                    CDI.current().select(TestIdentityAssociation.class).get().setPathBasedIdentity(true);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to setup @TestSecurity", e);
        }

    }

    private static Function<Permission, Uni<Boolean>> createPermissionChecker(String[] permissions) {
        record PermissionToAction(String permission, Set<String> actions) {
            void addAction(String action) {
                if (action != null)
                    actions.add(action);
            }
        }
        Map<String, PermissionToAction> permissionToActions = new HashMap<>();
        for (String perm : permissions) {
            if (perm.isEmpty()) {
                throw new RuntimeException("Cannot specify empty permissions attribute in @TestSecurity annotation");
            }
            var actionSeparatorIdx = perm.indexOf(PERMISSION_TO_ACTION_SEPARATOR);
            final String permission;
            final String action;
            if (actionSeparatorIdx < 0) {
                // permission only
                permission = perm;
                action = null;
            } else {
                permission = perm.substring(0, actionSeparatorIdx);
                action = perm.substring(actionSeparatorIdx + 1);
            }
            // aggregate permission to actions
            permissionToActions.computeIfAbsent(permission, k -> new PermissionToAction(permission, new HashSet<>()))
                    .addAction(action);
        }
        var possessedPermissions = permissionToActions.values().stream()
                .map(pa -> new StringPermission(pa.permission(), pa.actions().toArray(String[]::new))).toList();
        return requiredPermission -> Uni.createFrom().item(
                possessedPermissions.stream().anyMatch(possessedPermission -> possessedPermission.implies(requiredPermission)));
    }

    private Optional<AnnotationContainer<TestSecurity>> getAnnotationContainer(QuarkusTestMethodContext context)
            throws Exception {
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
        Optional<AnnotationContainer<TestSecurity>> annotationContainerOptional = AnnotationUtils.findAnnotation(method,
                TestSecurity.class);
        if (annotationContainerOptional.isEmpty()) {
            annotationContainerOptional = AnnotationUtils.findAnnotation(original, TestSecurity.class);
        }
        return annotationContainerOptional;
    }

    private SecurityIdentity augment(SecurityIdentity identity, Annotation[] annotations) {
        Instance<TestSecurityIdentityAugmentor> producer = CDI.current().select(TestSecurityIdentityAugmentor.class);
        if (producer.isResolvable()) {
            return producer.get().augment(identity, annotations);
        }
        return identity;
    }
}
