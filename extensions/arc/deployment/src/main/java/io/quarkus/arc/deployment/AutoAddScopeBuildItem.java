package io.quarkus.arc.deployment;

import java.util.Collection;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.arc.processor.Annotations;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.builder.item.MultiBuildItem;

/**
 * This build item can be used to turn a class that is not annotated with a CDI scope annotation into a bean, i.e. the default
 * scope annotation is added automatically if conditions are met.
 */
public final class AutoAddScopeBuildItem extends MultiBuildItem {

    public static Builder builder() {
        return new Builder();
    }

    private final MatchPredicate matchPredicate;
    private final boolean containerServicesRequired;
    private final DotName defaultScope;
    private final boolean unremovable;
    private final String reason;

    private AutoAddScopeBuildItem(MatchPredicate matchPredicate, boolean containerServicesRequired,
            DotName defaultScope, boolean unremovable, String reason) {
        this.matchPredicate = matchPredicate;
        this.containerServicesRequired = containerServicesRequired;
        this.defaultScope = defaultScope;
        this.unremovable = unremovable;
        this.reason = reason;
    }

    public boolean isContainerServicesRequired() {
        return containerServicesRequired;
    }

    public DotName getDefaultScope() {
        return defaultScope;
    }

    public boolean isUnremovable() {
        return unremovable;
    }

    public String getReason() {
        return reason != null ? ": " + reason : "";
    }

    public boolean test(ClassInfo clazz, Collection<AnnotationInstance> annotations, IndexView index) {
        return matchPredicate.test(clazz, annotations, index);
    }

    public interface MatchPredicate {

        /**
         * @param clazz
         * @param annotations
         * @param index
         * @return {@code true} if the input arguments match the predicate,
         *         {@code false} otherwise
         */
        boolean test(ClassInfo clazz, Collection<AnnotationInstance> annotations, IndexView index);

        default MatchPredicate and(MatchPredicate other) {
            return new MatchPredicate() {
                @Override
                public boolean test(ClassInfo clazz, Collection<AnnotationInstance> annotations, IndexView index) {
                    return test(clazz, annotations, index) && other.test(clazz, annotations, index);
                }
            };
        }

    }

    public static class Builder {

        private MatchPredicate matchPredicate;
        private boolean requiresContainerServices;
        private DotName defaultScope;
        private boolean unremovable;
        private String reason;

        private Builder() {
            this.defaultScope = BuiltinScope.DEPENDENT.getName();
            this.unremovable = false;
            this.requiresContainerServices = false;
        }

        /**
         * At least one injection point or lifecycle callback must be declared in the class hierarchy. Otherwise, the scope
         * annotation is not added.
         * <p>
         * Note that the detection algorithm is just the best effort. Some inheritance rules defined by the spec are not
         * followed, e.g. per spec an initializer method is only inherited if not overriden. This method merely scans the
         * annotations.
         * 
         * @return self
         */
        public Builder requiresContainerServices() {
            this.requiresContainerServices = true;
            return this;
        }

        /**
         * The bean will be unremovable.
         * 
         * @see ArcConfig#removeUnusedBeans
         * @return self
         */
        public Builder unremovable() {
            this.unremovable = true;
            return this;
        }

        /**
         * Set a custom predicate.
         * 
         * @param predicate
         * @return self
         */
        public Builder match(MatchPredicate predicate) {
            this.matchPredicate = predicate;
            return this;
        }

        /**
         * The class must be annotated with the given annotation. Otherwise, the scope annotation is not added.
         * <p>
         * The final predicate is a short-circuiting logical AND of the previous predicate (if any) and this condition.
         * 
         * @param annotationName
         * @return self
         */
        public Builder isAnnotatedWith(DotName annotationName) {
            return and((clazz, annotations, index) -> Annotations.contains(annotations, annotationName));
        }

        /**
         * The class or any of its element must be annotated with the given annotation. Otherwise, the scope annotation is not
         * added.
         * <p>
         * The final predicate is a short-circuiting logical AND of the previous predicate (if any) and this condition.
         * 
         * 
         * @param annotationNames
         * @return self
         */
        public Builder containsAnnotations(DotName... annotationNames) {
            return and((clazz, annotations, index) -> {
                for (DotName annotation : annotationNames) {
                    if (clazz.annotations().containsKey(annotation)) {
                        return true;
                    }
                }
                return false;
            });
        }

        /**
         * The class must directly or indirectly implement the given interface.
         * <p>
         * The final predicate is a short-circuiting logical AND of the previous predicate (if any) and this condition.
         * 
         * @param interfaceName
         * @return self
         */
        public Builder implementsInterface(DotName interfaceName) {
            return and((clazz, annotations, index) -> {
                if (clazz.interfaceNames().contains(interfaceName)) {
                    return true;
                }
                DotName superName = clazz.superName();
                while (superName != null && !superName.equals(DotNames.OBJECT)) {
                    ClassInfo superClass = index.getClassByName(superName);
                    if (superClass != null) {
                        if (superClass.interfaceNames().contains(interfaceName)) {
                            return true;
                        }
                        superName = superClass.superName();
                    }
                }
                return false;
            });
        }

        /**
         * The scope annotation added to the class.
         * 
         * @param scopeAnnotationName
         * @return self
         */
        public Builder defaultScope(DotName scopeAnnotationName) {
            this.defaultScope = scopeAnnotationName;
            return this;
        }

        /**
         * The scope annotation added to the class.
         * 
         * @param scope
         * @return
         */
        public Builder defaultScope(BuiltinScope scope) {
            return defaultScope(scope.getName());
        }

        /**
         * Specify an optional reason description that is used in log messages.
         * 
         * @param reason
         * @return the reason why the scope annotation was added
         */
        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        private Builder and(MatchPredicate other) {
            if (matchPredicate == null) {
                matchPredicate = other;
            } else {
                matchPredicate = matchPredicate.and(other);
            }
            return this;
        }

        public AutoAddScopeBuildItem build() {
            if (matchPredicate == null) {
                throw new IllegalStateException("A matching predicate must be set!");
            }
            return new AutoAddScopeBuildItem(matchPredicate, requiresContainerServices, defaultScope, unremovable, reason);
        }
    }

}
