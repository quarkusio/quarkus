package io.quarkus.qute.deployment;

import java.util.function.Predicate;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Makes it possible to intentionally ignore some classes when performing type-safe checking.
 */
public final class TypeCheckExcludeBuildItem extends MultiBuildItem {

    private final Predicate<Check> predicate;

    public TypeCheckExcludeBuildItem(Predicate<Check> predicate) {
        this.predicate = predicate;
    }

    public Predicate<Check> getPredicate() {
        return predicate;
    }

    public static class Check {

        /**
         * The name of a property/method, e.g. {@code foo} and {@code ping} for expression {@code foo.ping(bar)}.
         */
        public final String name;

        /**
         * The matching class.
         */
        public final ClassInfo clazz;

        /**
         * The number of parameters for methods.
         */
        public final int numberOfParameters;

        public Check(String name, ClassInfo clazz, int parameters) {
            this.name = name;
            this.clazz = clazz;
            this.numberOfParameters = parameters;
        }

        public boolean nameEquals(String... values) {
            for (String value : values) {
                if (name.equals(value)) {
                    return true;
                }
            }
            return false;
        }

    }

}
