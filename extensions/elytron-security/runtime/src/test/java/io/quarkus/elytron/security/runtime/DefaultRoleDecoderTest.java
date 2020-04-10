package io.quarkus.elytron.security.runtime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wildfly.security.authz.Attributes;
import org.wildfly.security.authz.AuthorizationIdentity;
import org.wildfly.security.authz.MapAttributes;
import org.wildfly.security.authz.Roles;

public class DefaultRoleDecoderTest {

    private static final String ATTRIBUTE_NAME_GROUPS = "groups";
    private static final String ATTRIBUTE_NAME_ROLES = "roles";

    private static final String ROLE_ADMIN = "Admin";
    private static final String ROLE_TESTER = "Tester";

    @Test
    public void testDecodeRoles() {
        Attributes attributes = new MapAttributes();
        attributes.add(ATTRIBUTE_NAME_GROUPS, 0, ROLE_ADMIN);
        attributes.add(ATTRIBUTE_NAME_ROLES, 0, ROLE_TESTER);
        AuthorizationIdentity authIdentity = AuthorizationIdentity.basicIdentity(attributes);
        DefaultRoleDecoder defaultRoleDecoder = new DefaultRoleDecoder();

        defaultRoleDecoder.groupsAttribute = ATTRIBUTE_NAME_GROUPS;
        Roles roles = defaultRoleDecoder.fromDefaultAttribute(authIdentity);
        Assertions.assertFalse(roles.isEmpty());
        Assertions.assertTrue(roles.contains(ROLE_ADMIN));
        Assertions.assertFalse(roles.contains(ROLE_TESTER));

        defaultRoleDecoder.groupsAttribute = ATTRIBUTE_NAME_ROLES;
        roles = defaultRoleDecoder.fromDefaultAttribute(authIdentity);
        Assertions.assertFalse(roles.isEmpty());
        Assertions.assertFalse(roles.contains(ROLE_ADMIN));
        Assertions.assertTrue(roles.contains(ROLE_TESTER));
    }
}
