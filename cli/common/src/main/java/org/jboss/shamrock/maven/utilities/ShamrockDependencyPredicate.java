package org.jboss.shamrock.maven.utilities;

import org.apache.maven.model.Dependency;

import java.util.function.Predicate;

import static org.jboss.shamrock.maven.utilities.MojoUtils.getPluginGroupId;

public class ShamrockDependencyPredicate implements Predicate<Dependency> {
    @Override
    public boolean test(final Dependency d) {
        return d.getGroupId().equalsIgnoreCase(getPluginGroupId());
    }
}
