package org.jboss.shamrock.maven.utilities;

import static org.jboss.shamrock.maven.utilities.MojoUtils.getPluginGroupId;

import java.util.function.Predicate;

import org.apache.maven.model.Dependency;

public class ShamrockDependencyPredicate implements Predicate<Dependency> {
    @Override
    public boolean test(final Dependency d) {
        return d.getGroupId().equalsIgnoreCase(getPluginGroupId());
    }
}
