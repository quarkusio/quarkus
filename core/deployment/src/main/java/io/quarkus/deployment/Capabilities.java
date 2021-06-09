package io.quarkus.deployment;

import java.util.HashSet;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.builditem.CapabilityBuildItem;

/**
 * This build items holds the set of registered capabilities.
 *
 * @see CapabilityBuildItem
 */
public final class Capabilities extends SimpleBuildItem {

    private final Set<String> capabilities;
    private final Set<String> capabilityPrefixes;

    public Capabilities(Set<String> capabilities) {
        this.capabilities = capabilities;
        this.capabilityPrefixes = new HashSet<>();
        for (String c : capabilities) {
            int i = 0;
            while (i >= 0) {
                i = c.indexOf('.', i + 1);
                if (i > 0) {
                    capabilityPrefixes.add(c.substring(0, i));
                }
            }
            capabilityPrefixes.add(c);
        }
    }

    public Set<String> getCapabilities() {
        return capabilities;
    }

    // @deprecated in 1.14.0.Final
    @Deprecated
    public boolean isCapabilityPresent(String capability) {
        return isPresent(capability);
    }

    /**
     * Checks whether a given capability is present during the build.
     *
     * @param capability capability name
     * @return true, in case the capability is present, otherwise - false
     */
    public boolean isPresent(String capability) {
        return capabilities.contains(capability);
    }

    /**
     * Checks whether a given capability is missing during the build.
     *
     * @param capability capability name
     * @return true, in case the capability is missing, otherwise - false
     */
    public boolean isMissing(String capability) {
        return !isPresent(capability);
    }

    /**
     * Checks whether a capability with a given prefix is present during the build.
     * <p>
     * A capability name is a dot-separated string. A prefix is also a string that is
     * composed of either the first capability name element or a dot separated sequence
     * of the capability name elements starting from the first one.
     * <p>
     * E.g. for capability {@code io.quarkus.resteasy.json.jackson} the following prefixes will
     * be registered:
     * <ul>
     * <li>{@code io}</li>
     * <li>{@code io.quarkus}</li>
     * <li>{@code io.quarkus.resteasy}</li>
     * <li>{@code io.quarkus.resteasy.json}</li>
     * </ul>
     * And this method could be used to check whether a capability with prefix, e.g.,
     * {@code io.quarkus.resteasy.json} is present during the build.
     * <p>
     * Given that only a single provider of a given capability is allowed in an application,
     * capability prefixes allow expressing a certain common aspect among different but
     * somewhat related capabilities.
     * <p>
     * E.g. there could be extensions providing the following capabilities:
     * <ul>
     * <li>{@code io.quarkus.resteasy.json.jackson}</li>
     * <li>{@code io.quarkus.resteasy.json.jackson.client}</li>
     * <li>{@code io.quarkus.resteasy.json.jsonb}</li>
     * <li>{@code io.quarkus.resteasy.json.jsonb.client}</li>
     * </ul>
     * Including any one of those extensions in an application will enable the RESTEasy JSON serializer.
     * In case a build step needs to check whether the RESTEasy JSON serializer is already enabled in an
     * application, instead of checking whether any of those capabilities is present, it could
     * simply check whether an extension with prefix {@code io.quarkus.resteasy.json} is present.
     *
     * @param capabilityPrefix capability prefix
     * @return true, in case the capability with the given prefix is present, otherwise - false
     */
    public boolean isCapabilityWithPrefixPresent(String capabilityPrefix) {
        return capabilityPrefixes.contains(capabilityPrefix);
    }

    /**
     * Checks whether a capability with a given prefix is missing during the build.
     * This method simple calls {@link #isCapabilityWithPrefixPresent(String)} and returns
     * its result inverted.
     *
     * @param capabilityPrefix capability prefix
     * @return true, in case the capability with the given prefix is missing, otherwise - false
     */
    public boolean isCapabilityWithPrefixMissing(String capabilityPrefix) {
        return !isCapabilityWithPrefixPresent(capabilityPrefix);
    }
}
