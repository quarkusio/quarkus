package io.quarkus.deployment.builditem;

import java.util.function.Predicate;

import org.jboss.builder.item.SimpleBuildItem;

/**
 * This is an optional build item that allows extensions to distinguish test classes from application classes. It is only
 * available during tests.
 */
public final class TestClassPredicateBuildItem extends SimpleBuildItem {

    private final Predicate<String> predicate;

    public TestClassPredicateBuildItem(Predicate<String> predicate) {
        this.predicate = predicate;
    }

    public Predicate<String> getPredicate() {
        return predicate;
    }

}
