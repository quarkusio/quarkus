package io.quarkus.hibernate.accessor.deployment;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.hibernate.accessor.deployment.HibernateAccessorRegistrationBuildItem.Builder;
import io.quarkus.hibernate.accessor.runtime.FieldAccess;
import io.quarkus.hibernate.accessor.runtime.ReflectionFreeAccessor;
import io.quarkus.hibernate.accessor.runtime.ReflectionFreeConstructorAccessor;
import io.quarkus.hibernate.accessor.runtime.ReflectionFreeFieldAccessor;
import io.quarkus.hibernate.accessor.runtime.ReflectionFreeMethodAccessor;

/** Resolves annotation overrides before producing any registration requests. */
final class HibernateAccessorAnnotationCollector {
    private static final DotName TYPE = DotName.createSimple(ReflectionFreeAccessor.class);
    private static final DotName FIELD = DotName.createSimple(ReflectionFreeFieldAccessor.class);
    private static final DotName METHOD = DotName.createSimple(ReflectionFreeMethodAccessor.class);
    private static final DotName CONSTRUCTOR = DotName.createSimple(ReflectionFreeConstructorAccessor.class);

    private static final DotName[] ANNOTATIONS_TO_INSPECT = new DotName[] { TYPE, FIELD, METHOD, CONSTRUCTOR };

    private HibernateAccessorAnnotationCollector() {
    }

    static Collection<HibernateAccessorRegistrationBuildItem> collect(IndexView index) {
        final Map<String, ClassInfo> classes = new TreeMap<>();
        for (DotName name : ANNOTATIONS_TO_INSPECT) {
            for (AnnotationInstance annotation : index.getAnnotations(name)) {
                ClassInfo declaringClass = switch (annotation.target().kind()) {
                    case CLASS -> annotation.target().asClass();
                    case FIELD -> annotation.target().asField().declaringClass();
                    case METHOD -> annotation.target().asMethod().declaringClass();
                    default -> throw new IllegalArgumentException("Unsupported accessor annotation target: " + annotation);
                };
                classes.put(declaringClass.name().toString(), declaringClass);
            }
        }
        return classes.values().stream().map(HibernateAccessorAnnotationCollector::collect).toList();
    }

    private static HibernateAccessorRegistrationBuildItem collect(ClassInfo type) {
        final Builder builder = new Builder(type);
        builder.all(type, new AnnotationMemberSelection(type, type.declaredAnnotation(TYPE), builder));
        return builder.build();
    }

    /**
     * Resolves, for each member the selection rules of {@link Builder#all(ClassInfo, FieldAccess)} accept, whether the
     * annotations
     * enable it and which field access they request. A member that those rules reject is an error when it
     * carries an enabled member annotation of its own.
     */
    private record AnnotationMemberSelection(ClassInfo type, AnnotationInstance typeAnnotation, Builder builder)
            implements
                Builder.MemberSelection {

        @Override
        public FieldAccess fieldAccess(FieldInfo field) {
            final AnnotationInstance direct = field.annotation(FIELD);
            final AnnotationInstance settings = settings(direct, "fields");
            if (!enabled(settings, typeAnnotation != null)) {
                return null;
            }
            builder.source((direct != null ? FIELD : TYPE) + " on " + type.name() + "#" + field.name());
            return settings == null || settings.value("access") == null ? FieldAccess.READ
                    : FieldAccess.valueOf(settings.value("access").asEnum());
        }

        @Override
        public boolean accepts(MethodInfo method) {
            final DotName memberAnnotation = memberAnnotation(method);
            final AnnotationInstance direct = method.annotation(memberAnnotation);
            if (!enabled(settings(direct, category(method)), typeAnnotation != null)) {
                return false;
            }
            builder.source((direct != null ? memberAnnotation : TYPE) + " on " + type.name() + "#" + method.name());
            return true;
        }

        @Override
        public void rejected(FieldInfo field) {
            if (explicitlyEnabled(field.annotation(FIELD))) {
                throw new IllegalArgumentException("Accessor field must be an instance field: " + field);
            }
        }

        @Override
        public void rejected(MethodInfo method) {
            if (!explicitlyEnabled(method.annotation(memberAnnotation(method)))) {
                return;
            }
            if (method.isConstructor()) {
                throw new IllegalArgumentException("Cannot register constructor of abstract class or enum: " + method);
            }
            throw new IllegalArgumentException(
                    "Accessor method must be an instance getter with no arguments and a non-void "
                            + "return type, or a setter with one argument: " + type.name() + "#" + method);
        }

        private AnnotationInstance settings(AnnotationInstance direct, String category) {
            return direct != null ? direct : nested(typeAnnotation, category);
        }

        private static DotName memberAnnotation(MethodInfo method) {
            return method.isConstructor() ? CONSTRUCTOR : METHOD;
        }

        private static String category(MethodInfo method) {
            return method.isConstructor() ? "constructors" : "methods";
        }

        private static boolean explicitlyEnabled(AnnotationInstance direct) {
            return direct != null && enabled(direct, true);
        }
    }

    private static AnnotationInstance nested(AnnotationInstance annotation, String name) {
        return annotation == null || annotation.value(name) == null ? null : annotation.value(name).asNested();
    }

    private static boolean enabled(AnnotationInstance settings, boolean typeAnnotated) {
        if (settings == null) {
            return typeAnnotated;
        } else {
            final AnnotationValue enabled = settings.value("enabled");
            return enabled == null || enabled.asBoolean();
        }
    }
}
