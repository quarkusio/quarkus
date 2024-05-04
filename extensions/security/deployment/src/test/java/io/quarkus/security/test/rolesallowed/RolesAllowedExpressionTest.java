package io.quarkus.security.test.rolesallowed;

import static io.quarkus.security.test.utils.IdentityMock.ADMIN;
import static io.quarkus.security.test.utils.IdentityMock.USER;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertFailureFor;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertSuccess;

import java.util.Collections;
import java.util.Set;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.test.utils.AuthData;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.test.QuarkusUnitTest;

public class RolesAllowedExpressionTest {

    private static final String APP_PROPS = "" +
            "sudo=admin\n" +
            "sec-group.user-part-one=user\n" +
            "sec-group.user-part-two=e\n" +
            "su=ad\n" +
            "do=min\n" +
            "spaces.s=s\n" +
            "spaces.p=p\n" +
            "spaces.a=a\n" +
            "spaces.c=c\n" +
            "multiple-roles-grp.1st=multiple-roles.1st\n" +
            "multiple-roles-grp.3rd=multiple-roles.3rd\n" +
            "test-profile-admin=batman\n" +
            "%test.test-profile-admin=admin\n" +
            "missing-profile-profile-admin=superman\n" +
            "%missing-profile.missing-profile-profile-admin=admin\n" +
            "all-roles=Administrator,Software,Tester,User\n" +
            "ldap-roles=cn=Administrator\\\\,ou=Software\\\\,dc=Tester\\\\,dc=User\n";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(RolesAllowedBean.class, IdentityMock.class,
                            AuthData.class, SecurityTestUtils.class, SecuredUtils.class)
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties"));

    @Inject
    RolesAllowedBean bean;

    @Test
    public void shouldRestrictAccessToSpecificRole() {
        // resolve role from 'sudo' config property
        assertSuccess(() -> bean.admin(), "accessibleForAdminOnly", ADMIN);
        // use default value 'user' as config property 'kudos' is missing
        assertSuccess(() -> bean.user(), "accessibleForUserOnly", USER);
        // composed expression
        assertSuccess(() -> bean.user2(), "accessibleForUserOnly2", USER);
        // multiple expressions
        assertSuccess(() -> bean.admin2(), "accessibleForAdminOnly2", ADMIN);
        // spaces
        assertSuccess(() -> bean.spaces(), "accessibleForSpacesOnly",
                new AuthData(Collections.singleton("s p a c e s"), false, "spaces"));
        // secured method allow multiple roles
        assertSuccess(() -> bean.multipleRoles(), "accessibleForMultipleRoles",
                new AuthData(Set.of("multiple-roles.1st"), false, "multiple-roles"));
        assertSuccess(() -> bean.multipleRoles(), "accessibleForMultipleRoles",
                new AuthData(Set.of("multiple-roles.2nd"), false, "multiple-roles"));
        assertSuccess(() -> bean.multipleRoles(), "accessibleForMultipleRoles",
                new AuthData(Set.of("multiple-roles.3rd"), false, "multiple-roles"));
        assertSuccess(() -> bean.multipleRoles(), "accessibleForMultipleRoles",
                new AuthData(Set.of("multiple-roles.4th"), false, "multiple-roles"));
        // test profile
        assertSuccess(() -> bean.testProfile(), "accessibleForTestProfileAdmin", ADMIN);
        assertFailureFor(() -> bean.missingTestProfile(), ForbiddenException.class, ADMIN);

        // property expression with collection separator should be treated as list
        assertSuccess(() -> bean.list(), "list",
                new AuthData(Set.of("Administrator"), false, "list"));
        assertSuccess(() -> bean.list(), "list",
                new AuthData(Set.of("Software"), false, "list"));
        assertSuccess(() -> bean.list(), "list",
                new AuthData(Set.of("Tester"), false, "list"));
        assertSuccess(() -> bean.list(), "list",
                new AuthData(Set.of("User"), false, "list"));
        assertSuccess(() -> bean.list(), "list",
                new AuthData(Set.of("Administrator", "Software", "Tester", "User"), false, "list"));
        assertFailureFor(() -> bean.list(), ForbiddenException.class, ADMIN);

        // property expression with escaped collection separator should not be treated as list
        assertSuccess(() -> bean.ldap(), "ldap",
                new AuthData(Set.of("cn=Administrator,ou=Software,dc=Tester,dc=User"), false, "ldap"));
    }

    @Test
    public void testStaticSecuredMethod() {
        assertSuccess(SecuredUtils::staticSecuredMethod, "admin", ADMIN);
        assertFailureFor(SecuredUtils::staticSecuredMethod, ForbiddenException.class, USER);
    }

    @Singleton
    public static class RolesAllowedBean {

        @RolesAllowed("${sudo}")
        public final String admin() {
            return "accessibleForAdminOnly";
        }

        @RolesAllowed("${su}${do}")
        public final String admin2() {
            return "accessibleForAdminOnly2";
        }

        @RolesAllowed("${kudos:user}")
        public final String user() {
            return "accessibleForUserOnly";
        }

        @RolesAllowed("${sec-group.user-part-on${sec-group.user-part-two}}")
        public final String user2() {
            return "accessibleForUserOnly2";
        }

        @RolesAllowed("${spaces.s} ${spaces.p} ${spaces.a} ${spaces.c} e s")
        public final String spaces() {
            return "accessibleForSpacesOnly";
        }

        @RolesAllowed({ "${multiple-roles-grp.1st}", "${multiple-roles-grp.2nd:multiple-roles.2nd}",
                "${multiple-roles-grp.3rd}", "multiple-roles.4th" })
        public final String multipleRoles() {
            return "accessibleForMultipleRoles";
        }

        @RolesAllowed("${test-profile-admin}")
        public final String testProfile() {
            return "accessibleForTestProfileAdmin";
        }

        @RolesAllowed("${missing-profile-profile-admin}")
        public final void missingTestProfile() {
            // should throw exception
        }

        @RolesAllowed("${all-roles}")
        public final String list() {
            return "list";
        }

        @RolesAllowed("${ldap-roles}")
        public final String ldap() {
            return "ldap";
        }

    }

    public static class SecuredUtils {

        private SecuredUtils() {
            // UTIL CLASS
        }

        @RolesAllowed("${sudo}")
        public static String staticSecuredMethod() {
            return ConfigProvider.getConfig().getValue("sudo", String.class);
        }

    }

}
