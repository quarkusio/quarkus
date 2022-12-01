package io.quarkus.security.deployment;

import org.jboss.jandex.MethodInfo;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.security.spi.runtime.SecurityCheck;

/**
 * Used as an integration point when extensions need to customize the security behavior of a bean
 * The ResultHandle that is returned by function needs to be an instance of SecurityCheck
 */
public final class AdditionalSecurityCheckBuildItem extends MultiBuildItem {

    private final MethodInfo methodInfo;
    private final SecurityCheck securityCheck;

    public AdditionalSecurityCheckBuildItem(MethodInfo methodInfo, SecurityCheck securityCheck) {
        this.methodInfo = methodInfo;
        this.securityCheck = securityCheck;
    }

    public MethodInfo getMethodInfo() {
        return methodInfo;
    }

    public SecurityCheck getSecurityCheck() {
        return securityCheck;
    }
}
