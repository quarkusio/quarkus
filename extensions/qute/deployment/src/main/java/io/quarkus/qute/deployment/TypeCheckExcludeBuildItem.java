package io.quarkus.qute.deployment;

import java.util.function.BiPredicate;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Makes it possible to intentionally ignore some classes when performing type-safe checking.
 */
public final class TypeCheckExcludeBuildItem extends MultiBuildItem {

    private final BiPredicate<String, ClassInfo> predicate;

    public TypeCheckExcludeBuildItem(BiPredicate<String, ClassInfo> predicate) {
        this.predicate = predicate;
    }

    public BiPredicate<String, ClassInfo> getPredicate() {
        return predicate;
    }

}
