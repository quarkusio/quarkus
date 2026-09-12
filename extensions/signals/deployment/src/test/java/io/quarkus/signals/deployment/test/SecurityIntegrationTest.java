package io.quarkus.signals.deployment.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.security.Authenticated;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.PermissionChecker;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.credential.PasswordCredential;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AnonymousAuthenticationRequest;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.signals.Receives;
import io.quarkus.signals.Signal;
import io.quarkus.signals.runtime.impl.SecurityIntegration;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Uni;

public class SecurityIntegrationTest extends AbstractSignalTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(
                    AuthenticatedCmd.class, MyClassSecuredReceivers.class, RolesAllowedCmd.class,
                    DenyAllCmd.class, PermitAllCmd.class, Cmd.class, SinglePermissionsAllowedCmd.class,
                    PublicCmd.class, MyMethodSecuredReceivers.class, MultiplePermissionsAllowedCmd.class,
                    SimpleTestIdentityProvider.class, SecurityIdentityExpirationCmd.class));

    @Inject
    Signal<AuthenticatedCmd> authenticatedCmdSignal;

    @Inject
    Signal<RolesAllowedCmd> rolesAllowedCmdSignal;

    @Inject
    Signal<PermitAllCmd> permitAllCmdSignal;

    @Inject
    Signal<DenyAllCmd> denyAllCmdSignal;

    @Inject
    Signal<PublicCmd> publicCmdSignal;

    @Inject
    Signal<SinglePermissionsAllowedCmd> singlePermissionsAllowedCmdSignal;

    @Inject
    Signal<MultiplePermissionsAllowedCmd> multiplePermissionsAllowedCmdSignal;

    @Inject
    Signal<SecurityIdentityExpirationCmd> securityIdentityExpirationCmdSignal;

    @Inject
    CurrentIdentityAssociation identityAssociation;

    @Inject
    IdentityProviderManager identityProviderManager;

    @ActivateRequestContext
    @Test
    public void testAuthenticatedOnReceiverClass() {
        MyClassSecuredReceivers.AUTHENTICATED_CMDS.clear();

        Stream.of("admin", "user").map(SecurityIntegrationTest::createSecurityIdentity).forEach(authenticatedUser -> {
            identityAssociation.setIdentity(authenticatedUser);
            String result = authenticatedCmdSignal.reactive().request(new AuthenticatedCmd("Hello"), String.class)
                    .ifNoItem().after(defaultTimeout()).fail()
                    .await().indefinitely();
            assertEquals("hello " + authenticatedUser.getPrincipal().getName(), result);
        });

        assertThrows(UnauthorizedException.class, () -> {
            identityAssociation.setIdentity(createSecurityIdentity(""));
            authenticatedCmdSignal.reactive().request(new AuthenticatedCmd("Hi"), String.class)
                    .ifNoItem().after(defaultTimeout()).fail()
                    .await().indefinitely();
        });

        assertEquals(2, MyClassSecuredReceivers.AUTHENTICATED_CMDS.size());
        assertTrue(MyClassSecuredReceivers.AUTHENTICATED_CMDS.stream().map(Cmd::value).allMatch("Hello"::equals));
    }

    @ActivateRequestContext
    @Test
    public void testMethodRolesAllowedOverridesAuthenticatedFromReceiverClass() {
        MyClassSecuredReceivers.ROLES_ALLOWED_CMDS.clear();

        // admin role -> access allowed
        identityAssociation.setIdentity(createSecurityIdentity("admin"));
        String result = rolesAllowedCmdSignal.reactive().request(new RolesAllowedCmd("Hello"), String.class)
                .ifNoItem().after(defaultTimeout()).fail()
                .await().indefinitely();
        assertEquals("hello admin", result);

        // authenticated, but no admin role -> forbidden access
        assertThrows(ForbiddenException.class, () -> {
            identityAssociation.setIdentity(createSecurityIdentity("user"));
            rolesAllowedCmdSignal.reactive().request(new RolesAllowedCmd("Hi"), String.class)
                    .ifNoItem().after(defaultTimeout()).fail()
                    .await().indefinitely();
        });

        // not authenticated -> forbidden access
        // - anonymous identity
        assertThrows(UnauthorizedException.class, () -> {
            identityAssociation.setIdentity(createSecurityIdentity(""));
            rolesAllowedCmdSignal.reactive().request(new RolesAllowedCmd("Hi"), String.class)
                    .ifNoItem().after(defaultTimeout()).fail()
                    .await().indefinitely();
        });
        // - no identity (-> effectively anonymous identity)
        assertThrows(UnauthorizedException.class, () -> {
            identityAssociation.setIdentity((SecurityIdentity) null);
            rolesAllowedCmdSignal.reactive().request(new RolesAllowedCmd("Hi"), String.class)
                    .ifNoItem().after(defaultTimeout()).fail()
                    .await().indefinitely();
        });

        assertEquals(1, MyClassSecuredReceivers.ROLES_ALLOWED_CMDS.size());
        assertEquals("Hello", MyClassSecuredReceivers.ROLES_ALLOWED_CMDS.get(0).value());
    }

    @ActivateRequestContext
    @Test
    public void testMethodPermitAllOverridesReceivedClassAuthenticated() {
        testPermitAllAnnotation();
    }

    @ActivateRequestContext
    @Test
    public void testMethodDenyAllOverridesReceivedClassAuthenticated() {
        MyClassSecuredReceivers.DENY_ALL_CMDS.clear();

        assertThrows(UnauthorizedException.class, () -> {
            identityAssociation.setIdentity(createSecurityIdentity(""));
            denyAllCmdSignal.reactive().request(new DenyAllCmd("Hi"), String.class)
                    .ifNoItem().after(defaultTimeout()).fail()
                    .await().indefinitely();
        });
        assertThrows(ForbiddenException.class, () -> {
            identityAssociation.setIdentity(createSecurityIdentity("admin"));
            denyAllCmdSignal.reactive().request(new DenyAllCmd("Hi"), String.class)
                    .ifNoItem().after(defaultTimeout()).fail()
                    .await().indefinitely();
        });

        assertTrue(MyClassSecuredReceivers.DENY_ALL_CMDS.isEmpty());
    }

    @ActivateRequestContext
    @Test
    public void testPublicMethodNotAuthorized() {
        MyMethodSecuredReceivers.PUBLIC_CMDS.clear();

        identityAssociation.setIdentity((SecurityIdentity) null);
        String result = publicCmdSignal.reactive().request(new PublicCmd("Hello"), String.class)
                .ifNoItem().after(defaultTimeout()).fail()
                .await().indefinitely();
        assertEquals("hello ", result);

        // test security identity access from a method when no authorization was performed
        identityAssociation.setIdentity(createSecurityIdentity("Martin"));
        result = publicCmdSignal.reactive().request(new PublicCmd("Hello"), String.class)
                .ifNoItem().after(defaultTimeout()).fail()
                .await().indefinitely();
        assertEquals("hello Martin", result);

        assertEquals(2, MyMethodSecuredReceivers.PUBLIC_CMDS.size());
        assertTrue(MyMethodSecuredReceivers.PUBLIC_CMDS.stream().map(Cmd::value).allMatch("Hello"::equals));
    }

    @ActivateRequestContext
    @Test
    public void testSingleMethodPermissionsAllowedApplied() {
        MyMethodSecuredReceivers.SINGLE_PERMISSIONS_ALLOWED_CMDS.clear();

        identityAssociation.setIdentity(createSecurityIdentity("single"));
        String result = singlePermissionsAllowedCmdSignal.reactive()
                .request(new SinglePermissionsAllowedCmd("Hello"), String.class)
                .ifNoItem().after(defaultTimeout()).fail()
                .await().indefinitely();
        assertEquals("hello single", result);

        assertThrows(UnauthorizedException.class, () -> {
            identityAssociation.setIdentity(createSecurityIdentity(""));
            singlePermissionsAllowedCmdSignal.reactive().request(new SinglePermissionsAllowedCmd("Hi"), String.class)
                    .ifNoItem().after(defaultTimeout()).fail()
                    .await().indefinitely();
        });
        assertThrows(ForbiddenException.class, () -> {
            identityAssociation.setIdentity(createSecurityIdentity("admin"));
            singlePermissionsAllowedCmdSignal.reactive().request(new SinglePermissionsAllowedCmd("Hi"), String.class)
                    .ifNoItem().after(defaultTimeout()).fail()
                    .await().indefinitely();
        });

        assertEquals(1, MyMethodSecuredReceivers.SINGLE_PERMISSIONS_ALLOWED_CMDS.size());
        assertEquals("Hello", MyMethodSecuredReceivers.SINGLE_PERMISSIONS_ALLOWED_CMDS.get(0).value());
    }

    @ActivateRequestContext
    @Test
    public void testMultipleMethodPermissionsAllowedApplied() {
        MyMethodSecuredReceivers.MULTIPLE_PERMISSIONS_ALLOWED_CMDS.clear();

        SecurityIdentity securityIdentity = identityProviderManager.authenticate(
                new UsernamePasswordAuthenticationRequest("quarkus", new PasswordCredential("quarkus".toCharArray())))
                .await().indefinitely();
        identityAssociation.setIdentity(securityIdentity);

        // has both permissions -> grant access
        String result = multiplePermissionsAllowedCmdSignal.reactive()
                .request(new MultiplePermissionsAllowedCmd("multiple-1-multiple-2"), String.class)
                .ifNoItem().after(defaultTimeout()).fail()
                .await().indefinitely();
        assertEquals("multiple-1-multiple-2 quarkus", result);

        // has neither permission and is anonymous -> unauthorized
        assertThrows(UnauthorizedException.class, () -> {
            var anonymous = identityProviderManager.authenticate(AnonymousAuthenticationRequest.INSTANCE).await()
                    .indefinitely();
            identityAssociation.setIdentity(anonymous);

            multiplePermissionsAllowedCmdSignal.reactive().request(new MultiplePermissionsAllowedCmd("Hi"), String.class)
                    .ifNoItem().after(defaultTimeout()).fail()
                    .await().indefinitely();
        });
        // has neither permission and is authenticated -> forbidden
        assertThrows(ForbiddenException.class, () -> {
            identityAssociation.setIdentity(securityIdentity);
            multiplePermissionsAllowedCmdSignal.reactive().request(new MultiplePermissionsAllowedCmd("Hi"), String.class)
                    .ifNoItem().after(defaultTimeout()).fail()
                    .await().indefinitely();
        });
        // has one of the permissions and is authenticated -> forbidden
        assertThrows(ForbiddenException.class, () -> {
            identityAssociation.setIdentity(securityIdentity);
            multiplePermissionsAllowedCmdSignal.reactive()
                    .request(new MultiplePermissionsAllowedCmd("multiple-1"), String.class)
                    .ifNoItem().after(defaultTimeout()).fail()
                    .await().indefinitely();
        });
        // has the other one of the permissions and is authenticated -> forbidden
        assertThrows(ForbiddenException.class, () -> {
            identityAssociation.setIdentity(securityIdentity);
            multiplePermissionsAllowedCmdSignal.reactive()
                    .request(new MultiplePermissionsAllowedCmd("multiple-2"), String.class)
                    .ifNoItem().after(defaultTimeout()).fail()
                    .await().indefinitely();
        });

        assertEquals(1, MyMethodSecuredReceivers.MULTIPLE_PERMISSIONS_ALLOWED_CMDS.size());
        assertEquals("multiple-1-multiple-2", MyMethodSecuredReceivers.MULTIPLE_PERMISSIONS_ALLOWED_CMDS.get(0).value());
    }

    @Test
    public void testAccessDeniedForAuthenticatedMethodWhenCdiRequestContextNotActive() {
        MyClassSecuredReceivers.AUTHENTICATED_CMDS.clear();

        assertThrows(UnauthorizedException.class, () -> {
            authenticatedCmdSignal.reactive().request(new AuthenticatedCmd("Hi"), String.class)
                    .ifNoItem().after(defaultTimeout()).fail()
                    .await().indefinitely();
        });

        assertTrue(MyClassSecuredReceivers.AUTHENTICATED_CMDS.isEmpty());
    }

    @Test
    public void testAccessGrantedForPermitAllMethodWhenCdiRequestContextNotActive() {
        testPermitAllAnnotation();
    }

    @ActivateRequestContext
    @Test
    public void testSecurityIdentityExpiration() {
        MyMethodSecuredReceivers.SECURITY_IDENTITY_EXPIRATION_CMDS.clear();

        // security identity not expired
        SecurityIdentity securityIdentity = QuarkusSecurityIdentity.builder(createSecurityIdentity("Sergey"))
                .addAttribute(SecurityIntegration.QUARKUS_IDENTITY_EXPIRE_TIME, Instant.now().getEpochSecond() + 100)
                .build();
        identityAssociation.setIdentity(securityIdentity);
        String result = securityIdentityExpirationCmdSignal.reactive()
                .request(new SecurityIdentityExpirationCmd("Hey"), String.class)
                .ifNoItem().after(defaultTimeout()).fail()
                .await().indefinitely();
        assertEquals("hey Sergey", result);

        // security identity expired
        securityIdentity = QuarkusSecurityIdentity.builder(createSecurityIdentity("Sergey"))
                .addAttribute(SecurityIntegration.QUARKUS_IDENTITY_EXPIRE_TIME, Instant.now().getEpochSecond() - 2)
                .build();
        identityAssociation.setIdentity(securityIdentity);
        assertThrows(UnauthorizedException.class, () -> securityIdentityExpirationCmdSignal
                .reactive().request(new SecurityIdentityExpirationCmd("Hi"), String.class)
                .ifNoItem().after(defaultTimeout()).fail()
                .await().indefinitely());

        assertEquals(1, MyMethodSecuredReceivers.SECURITY_IDENTITY_EXPIRATION_CMDS.size());
        assertEquals("Hey", MyMethodSecuredReceivers.SECURITY_IDENTITY_EXPIRATION_CMDS.get(0).value());
    }

    private void testPermitAllAnnotation() {
        MyClassSecuredReceivers.PERMIT_ALL_CMDS.clear();

        if (Arc.requireContainer().requestContext().isActive()) {
            identityAssociation.setIdentity((SecurityIdentity) null);
        }

        String result = permitAllCmdSignal.reactive().request(new PermitAllCmd("Hello"), String.class)
                .ifNoItem().after(defaultTimeout()).fail()
                .await().indefinitely();
        assertEquals("hello ", result);

        assertEquals(1, MyClassSecuredReceivers.PERMIT_ALL_CMDS.size());
        assertEquals("Hello", MyClassSecuredReceivers.PERMIT_ALL_CMDS.get(0).value());
    }

    private static SecurityIdentity createSecurityIdentity(String name) {
        return QuarkusSecurityIdentity.builder()
                .setAnonymous(name.isEmpty())
                .setPrincipal(new QuarkusPrincipal(name))
                .addRole(name)
                .addPermissionAsString(name.isEmpty() ? "ignored" : name)
                .build();
    }

    // --- Signal types ---

    interface Cmd {

        String value();

    }

    record AuthenticatedCmd(String value) implements Cmd {
    }

    record RolesAllowedCmd(String value) implements Cmd {
    }

    record PermitAllCmd(String value) implements Cmd {
    }

    record DenyAllCmd(String value) implements Cmd {
    }

    record PublicCmd(String value) implements Cmd {
    }

    record SinglePermissionsAllowedCmd(String value) implements Cmd {
    }

    record MultiplePermissionsAllowedCmd(String value) implements Cmd {
    }

    record SecurityIdentityExpirationCmd(String value) implements Cmd {

    }

    // --- Receivers ---

    @Authenticated
    @Singleton
    public static class MyClassSecuredReceivers {

        static final List<Cmd> AUTHENTICATED_CMDS = new CopyOnWriteArrayList<>();
        static final List<Cmd> ROLES_ALLOWED_CMDS = new CopyOnWriteArrayList<>();
        static final List<Cmd> PERMIT_ALL_CMDS = new CopyOnWriteArrayList<>();
        static final List<Cmd> DENY_ALL_CMDS = new CopyOnWriteArrayList<>();

        @Inject
        SecurityIdentity securityIdentity;

        String process(@Receives AuthenticatedCmd cmd) {
            return process(cmd, AUTHENTICATED_CMDS);
        }

        @RolesAllowed("admin")
        String process(@Receives RolesAllowedCmd cmd) {
            return process(cmd, ROLES_ALLOWED_CMDS);
        }

        @DenyAll
        String process(@Receives DenyAllCmd cmd) {
            return process(cmd, DENY_ALL_CMDS);
        }

        @PermitAll
        String process(@Receives PermitAllCmd cmd) {
            return process(cmd, PERMIT_ALL_CMDS);
        }

        private String process(Cmd cmd, Collection<Cmd> cmds) {
            cmds.add(cmd);
            final String principalName;
            if (Arc.requireContainer().requestContext().isActive()) {
                principalName = securityIdentity.getPrincipal().getName();
            } else {
                principalName = "";
            }
            return cmd.value().toLowerCase() + " " + principalName;
        }

    }

    @Singleton
    public static final class SimpleTestIdentityProvider implements IdentityProvider<UsernamePasswordAuthenticationRequest> {

        @Override
        public Class<UsernamePasswordAuthenticationRequest> getRequestType() {
            return UsernamePasswordAuthenticationRequest.class;
        }

        @Override
        public Uni<SecurityIdentity> authenticate(UsernamePasswordAuthenticationRequest request,
                AuthenticationRequestContext context) {
            if ("quarkus".equals(request.getUsername()) && "quarkus".equals(new String(request.getPassword().getPassword()))) {
                return Uni.createFrom().item(createSecurityIdentity("quarkus"));
            }
            return Uni.createFrom().nullItem();
        }
    }

    public static class MyMethodSecuredReceivers {

        static final List<Cmd> SINGLE_PERMISSIONS_ALLOWED_CMDS = new CopyOnWriteArrayList<>();
        static final List<Cmd> MULTIPLE_PERMISSIONS_ALLOWED_CMDS = new CopyOnWriteArrayList<>();
        static final List<Cmd> PUBLIC_CMDS = new CopyOnWriteArrayList<>();
        static final List<Cmd> SECURITY_IDENTITY_EXPIRATION_CMDS = new CopyOnWriteArrayList<>();

        @Inject
        SecurityIdentity securityIdentity;

        @PermitAll
        String process(@Receives PublicCmd cmd) {
            return process(cmd, PUBLIC_CMDS);
        }

        @PermissionsAllowed("single")
        String process(@Receives SinglePermissionsAllowedCmd cmd) {
            return process(cmd, SINGLE_PERMISSIONS_ALLOWED_CMDS);
        }

        @PermissionsAllowed("multiple-1")
        @PermissionsAllowed("multiple-2")
        String process(@Receives MultiplePermissionsAllowedCmd cmd) {
            return process(cmd, MULTIPLE_PERMISSIONS_ALLOWED_CMDS);
        }

        @Authenticated
        String process(@Receives SecurityIdentityExpirationCmd cmd) {
            return process(cmd, SECURITY_IDENTITY_EXPIRATION_CMDS);
        }

        @PermissionChecker("multiple-1")
        boolean hasMultiple1Permission(MultiplePermissionsAllowedCmd cmd) {
            return cmd.value.startsWith("multiple-1");
        }

        @PermissionChecker("multiple-2")
        boolean hasMultiple2Permission(MultiplePermissionsAllowedCmd cmd) {
            return cmd.value.endsWith("multiple-2");
        }

        private String process(Cmd cmd, Collection<Cmd> cmds) {
            cmds.add(cmd);
            return cmd.value().toLowerCase() + " " + securityIdentity.getPrincipal().getName();
        }
    }

}
