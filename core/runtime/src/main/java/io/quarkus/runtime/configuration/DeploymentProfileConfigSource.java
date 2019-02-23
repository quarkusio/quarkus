package io.quarkus.runtime.configuration;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.wildfly.common.Assert;

/**
 * A configuration source which supports deployment profiles.
 */
public class DeploymentProfileConfigSource extends AbstractDelegatingConfigSource {
    private final String profilePrefix;
    private NamesSet namesSet;

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

    public Set<String> getPropertyNames() {
        final NamesSet namesSet = this.namesSet;
        if (namesSet == null)
            return this.namesSet = new NamesSet(getDelegate().getPropertyNames());
        return namesSet;
    }

    public String getValue(final String name) {
        final ConfigSource delegate = getDelegate();
        if (name.startsWith("%")) {
            // disallow "cheating"
            return null;
        } else {
            final String nameWithProfile = profilePrefix + name;
            return delegate.getPropertyNames().contains(nameWithProfile) ? delegate.getValue(nameWithProfile)
                    : delegate.getValue(name);
        }
    }

    public String getName() {
        return delegate.getName();
    }

    final class NamesSet extends AbstractSet<String> {
        private final Set<String> delegateSet;

        NamesSet(final Set<String> delegateSet) {
            this.delegateSet = delegateSet;
        }

        public Iterator<String> iterator() {
            return new Itr(delegateSet.iterator());
        }

        public int size() {
            // very slow, unfortunately
            int cnt = 0;
            for (String ignored : this)
                cnt++;
            return cnt;
        }

        final class Itr implements Iterator<String> {
            private final Iterator<String> iterator;
            private String next;

            Itr(final Iterator<String> iterator) {
                this.iterator = iterator;
            }

            public boolean hasNext() {
                final String profilePrefix = DeploymentProfileConfigSource.this.profilePrefix;
                final Iterator<String> iterator = this.iterator;
                while (next == null) {
                    if (!iterator.hasNext())
                        return false;
                    final String test = iterator.next();
                    if (test.startsWith("%")) {
                        final int prefixLen = profilePrefix.length();
                        if (test.startsWith(profilePrefix)) {
                            next = test.substring(prefixLen);
                            return true;
                        }
                    } else if (!delegateSet.contains(profilePrefix + test)) {
                        next = test;
                        return true;
                    }
                }
                return true;
            }

            public String next() {
                if (!hasNext())
                    throw new NoSuchElementException();
                return next;
            }
        }
    }
}
