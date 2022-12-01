package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Build item that will allow the use of JNDI by default.
 * <p>
 * This should only be provided by extensions that rely on JNDI as a core part of the extension (e.g. if LDAP is a core part of
 * what the extension does).
 */
public final class AllowJNDIBuildItem extends MultiBuildItem {
}
