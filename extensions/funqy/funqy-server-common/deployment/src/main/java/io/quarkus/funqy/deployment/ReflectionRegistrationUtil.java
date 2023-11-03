package io.quarkus.funqy.deployment;

import java.util.function.Predicate;

import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;

import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;

final class ReflectionRegistrationUtil {

    private ReflectionRegistrationUtil() {
    }

    static final IgnoreTypeForReflectionPredicate IGNORE_TYPE_FOR_REFLECTION_PREDICATE = new IgnoreTypeForReflectionPredicate();
    static final IgnoreFieldForReflectionPredicate IGNORE_FIELD_FOR_REFLECTION_PREDICATE = new IgnoreFieldForReflectionPredicate();
    static final IgnoreMethodForReflectionPredicate IGNORE_METHOD_FOR_REFLECTION_PREDICATE = new IgnoreMethodForReflectionPredicate();

    private static final DotName JSON_IGNORE = DotName.createSimple("com.fasterxml.jackson.annotation.JsonIgnore");
    private static final DotName JSONB_TRANSIENT = DotName.createSimple("jakarta.json.bind.annotation.JsonbTransient");

    private static class IgnoreTypeForReflectionPredicate implements Predicate<DotName> {

        @Override
        public boolean test(DotName dotName) {
            if (ReflectiveHierarchyBuildItem.DefaultIgnoreTypePredicate.INSTANCE.test(dotName)) {
                return true;
            }

            String name = dotName.toString();
            for (String packageName : PACKAGES_IGNORED_FOR_REFLECTION) {
                if (name.startsWith(packageName)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static class IgnoreFieldForReflectionPredicate implements Predicate<FieldInfo> {

        @Override
        public boolean test(FieldInfo fieldInfo) {
            return fieldInfo.hasAnnotation(JSON_IGNORE)
                    || fieldInfo.hasAnnotation(JSONB_TRANSIENT);
        }
    }

    private static class IgnoreMethodForReflectionPredicate implements Predicate<MethodInfo> {

        @Override
        public boolean test(MethodInfo methodInfo) {
            return methodInfo.hasAnnotation(JSON_IGNORE)
                    || methodInfo.hasAnnotation(JSONB_TRANSIENT);
        }
    }

    private static final String[] PACKAGES_IGNORED_FOR_REFLECTION = {
            // Mutiny
            "io.smallrye.mutiny."
    };
}
