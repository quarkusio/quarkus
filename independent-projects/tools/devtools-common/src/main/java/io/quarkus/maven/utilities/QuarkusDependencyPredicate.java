package io.quarkus.maven.utilities;

import java.util.function.Predicate;

import org.apache.maven.model.Dependency;

public class QuarkusDependencyPredicate implements Predicate<Dependency> {
    @Override
    public boolean test(final Dependency d) {
        return d.getGroupId().equalsIgnoreCase("io.quarkus");
    }
}
