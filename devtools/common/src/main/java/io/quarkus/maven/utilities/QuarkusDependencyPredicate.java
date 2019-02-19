package io.quarkus.maven.utilities;

import org.apache.maven.model.Dependency;

import java.util.function.Predicate;

import static io.quarkus.maven.utilities.MojoUtils.getPluginGroupId;

public class QuarkusDependencyPredicate implements Predicate<Dependency> {
    @Override
    public boolean test(final Dependency d) {
        return d.getGroupId().equalsIgnoreCase(getPluginGroupId());
    }
}
