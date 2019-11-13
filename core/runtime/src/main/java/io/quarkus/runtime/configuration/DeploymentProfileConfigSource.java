package io.quarkus.runtime.configuration;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.function.UnaryOperator;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.wildfly.common.Assert;

/**
 * A configuration source which supports deployment profiles.
 */
public class DeploymentProfileConfigSource extends AbstractDelegatingConfigSource {
    private static final long serialVersionUID = -8001338475089294128L;

    private final String profilePrefix;

    public static UnaryOperator<ConfigSource> wrapper() {
        return new UnaryOperator<ConfigSource>() {
            @Override
            public ConfigSource apply(ConfigSource configSource) {
                return new DeploymentProfileConfigSource(configSource, ProfileManager.getActiveProfile());
            }
        };
    }

    /**
     * Construct a new instance.
     *
     * @param delegate the delegate configuration source (must not be {@code null})
     * @param profileName the profile name (must not be {@code null})
     */
    public DeploymentProfileConfigSource(final ConfigSource delegate, final String profileName) {
        super(delegate);
        Assert.checkNotNullParam("profileName", profileName);
        profilePrefix = "%" + profileName + ".";
    }

    Object writeReplace() throws ObjectStreamException {
        return new Ser(delegate, profilePrefix);
    }

    static final class Ser implements Serializable {
        private static final long serialVersionUID = -4618790131794331510L;

        final ConfigSource d;
        final String p;

        Ser(final ConfigSource d, String p) {
            this.d = d;
            this.p = p;
        }

        Object readResolve() {
            return new DeploymentProfileConfigSource(d, p);
        }
    }

    public Set<String> getPropertyNames() {
        Set<String> propertyNames = delegate.getPropertyNames();
        //if a key is only present in a profile we still want the unprofiled key name to show up
        Set<String> ret = new HashSet<>(propertyNames);
        for (String i : propertyNames) {
            if (i.startsWith(profilePrefix)) {
                ret.add(i.substring(profilePrefix.length()));
            }
        }
        return ret;
    }

    public String getValue(final String name) {
        final ConfigSource delegate = getDelegate();
        final String nameWithProfile = profilePrefix + name;
        String result = delegate.getValue(nameWithProfile);
        if (result != null) {
            return result;
        } else {
            return delegate.getValue(name);
        }
    }

    public String getName() {
        return delegate.getName();
    }

    public String toString() {
        return "DeploymentProfileConfigSource[profile=" + profilePrefix + ",delegate=" + getDelegate() + ",ord=" + getOrdinal()
                + "]";
    }
}
