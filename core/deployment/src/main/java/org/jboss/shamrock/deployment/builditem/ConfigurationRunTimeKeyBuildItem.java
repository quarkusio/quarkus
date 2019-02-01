package org.jboss.shamrock.deployment.builditem;

import java.util.EnumSet;

import org.jboss.builder.item.MultiBuildItem;
import org.jboss.shamrock.deployment.configuration.CompoundConfigType;
import org.jboss.shamrock.runtime.annotations.ConfigPhase;
import org.wildfly.common.Assert;

/**
 * A configuration key which is used by the extension at run time.
 */
public final class ConfigurationRunTimeKeyBuildItem extends MultiBuildItem {
    private final String baseAddress;
    private final EnumSet<ConfigPhase> configPhases;
    private final CompoundConfigType expectedType;

    public ConfigurationRunTimeKeyBuildItem(final String baseAddress, final EnumSet<ConfigPhase> configPhases, final CompoundConfigType expectedType) {
        Assert.checkNotNullParam("baseAddress", baseAddress);
        Assert.checkNotNullParam("configPhases", configPhases);
        Assert.checkNotNullParam("expectedType", expectedType);
        this.baseAddress = baseAddress;
        this.configPhases = configPhases;
        this.expectedType = expectedType;
    }

    public String getBaseAddress() {
        return baseAddress;
    }

    public EnumSet<ConfigPhase> getConfigPhases() {
        return configPhases;
    }

    public CompoundConfigType getExpectedType() {
        return expectedType;
    }
}
