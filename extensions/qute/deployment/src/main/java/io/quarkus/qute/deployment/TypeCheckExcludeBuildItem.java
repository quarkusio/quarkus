package io.quarkus.qute.deployment;

import java.util.Objects;
import java.util.function.Predicate;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Makes it possible to intentionally ignore some parts of an expression when performing type-safe validation.
 *
 * @see TypeCheck
 */
public final class TypeCheckExcludeBuildItem extends MultiBuildItem {

    private final Predicate<TypeCheck> predicate;
    private final boolean extensionMethodPredicate;

    public TypeCheckExcludeBuildItem(Predicate<TypeCheck> predicate) {
        this.predicate = Objects.requireNonNull(predicate);
        this.extensionMethodPredicate = false;
    }

    public TypeCheckExcludeBuildItem(Predicate<TypeCheck> predicate, boolean extensionMethodPredicate) {
        this.predicate = predicate;
        this.extensionMethodPredicate = extensionMethodPredicate;
    }

    public Predicate<TypeCheck> getPredicate() {
        return predicate;
    }

    /**
     * It might come handy to exclude {@link io.quarkus.qute.TemplateExtension} method from validation based on resolved
     * return {@link Type} of the method. For example, type variables are hard to resolve in all situations and
     * {@link #extensionMethodPredicate} allows you to skip validation on your conditions, as we do for
     * {@link io.quarkus.qute.runtime.extensions.OrOperatorTemplateExtensions}.
     *
     * @return true if the predicate is used to exclude {@link io.quarkus.qute.TemplateExtension} method.
     */
    public boolean isExtensionMethodPredicate() {
        return extensionMethodPredicate;
    }

    /**
     * Represents a type check of a part of an expression.
     * <p>
     * For example, the expression {@code item.name} has two parts, {@code item} and {@code name}, and for each part a
     * new instance is created and tested for exclusion.
     */
    public static class TypeCheck {

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

        /**
         * The matching type.
         */
        public final Type type;

        public TypeCheck(String name, ClassInfo clazz, Type type, int parameters) {
            this.name = Objects.requireNonNull(name, "Name must not be null");
            this.clazz = clazz;
            this.numberOfParameters = parameters;
            this.type = type;
        }

        public boolean nameIn(String... values) {
            for (String value : values) {
                if (name.equals(value)) {
                    return true;
                }
            }
            return false;
        }

        public boolean isProperty() {
            return numberOfParameters == -1;
        }

        public boolean classNameEquals(DotName name) {
            return clazz != null ? clazz.name().equals(name) : false;
        }

    }

}
