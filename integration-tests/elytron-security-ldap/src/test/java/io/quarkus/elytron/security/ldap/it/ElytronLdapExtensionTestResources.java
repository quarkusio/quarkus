package io.quarkus.elytron.security.ldap.it;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.ldap.LdapServerTestResource;

@WithTestResource(value = LdapServerTestResource.class, restrictToAnnotatedClass = false)
public class ElytronLdapExtensionTestResources {
}
