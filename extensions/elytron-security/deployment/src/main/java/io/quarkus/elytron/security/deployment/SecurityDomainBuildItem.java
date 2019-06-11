package io.quarkus.elytron.security.deployment;

import org.wildfly.security.auth.server.SecurityDomain;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * This represent a {@linkplain SecurityDomain} instance output by a build step.
 */
public final class SecurityDomainBuildItem extends SimpleBuildItem {

    private final RuntimeValue<SecurityDomain> securityDomain;

    public SecurityDomainBuildItem(RuntimeValue<SecurityDomain> securityDomain) {
        this.securityDomain = securityDomain;
    }

    public RuntimeValue<SecurityDomain> getSecurityDomain() {
        return securityDomain;
    }
}
