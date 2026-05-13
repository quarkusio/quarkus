package io.quarkus.deployment.dev;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

public class ClassComparisonUtil {
    private static final Set<DotName> IGNORED_ANNOTATIONS = Set.of(
            DotName.createSimple("kotlin.jvm.internal.SourceDebugExtension"),
            DotName.createSimple("kotlin.Metadata"));

    static boolean isSameStructure(ClassInfo clazz, ClassInfo old) {
        if (clazz.flags() != old.flags()) {
            return false;
        }
        if (!clazz.typeParameters().equals(old.typeParameters())) {
            return false;
        }
        if (!clazz.interfaceNames().equals(old.interfaceNames())) {
            return false;
        }
        if (!compareAnnotations(clazz.declaredAnnotations(), old.declaredAnnotations())) {
            return false;
        }
        if (old.fields().size() != clazz.fields().size()) {
            return false;
        }
        Map<String, FieldInfo> oldFields = old.fields().stream()
                .collect(Collectors.toMap(FieldInfo::name, Function.identity()));
        for (FieldInfo field : clazz.fields()) {
            FieldInfo of = oldFields.get(field.name());
            if (of == null) {
                return false;
            }
            if (of.flags() != field.flags()) {
                return false;
            }
            if (!of.type().equals(field.type())) {
                return false;
            }
            // Use declaredAnnotations() to only compare FIELD annotations, not TYPE annotations.
            // TYPE annotations are already compared via the type().equals() check above,
            // since Jandex Type objects include annotation positioning information.
            if (!compareAnnotations(of.declaredAnnotations(), field.declaredAnnotations())) {
                return false;
            }
        }
        List<MethodInfo> methods = clazz.methods();
        List<MethodInfo> oldMethods = old.methods();
        if (methods.size() != oldMethods.size()) {
            return false;
        }
        for (MethodInfo method : methods) {
            MethodInfo om = null;
            for (MethodInfo i : oldMethods) {
                if (!i.name().equals(method.name())) {
                    continue;
                }
                if (!i.returnType().equals(method.returnType())) {
                    continue;
                }
                if (i.parametersCount() != method.parametersCount()) {
                    continue;
                }
                if (i.flags() != method.flags()) {
                    continue;
                }
                if (!Objects.equals(i.defaultValue(), method.defaultValue())) {
                    continue;
                }
                boolean paramEqual = true;
                for (int j = 0; j < method.parametersCount(); ++j) {
                    Type a = method.parameterType(j);
                    Type b = i.parameterType(j);
                    if (!a.equals(b)) {
                        paramEqual = false;
                        break;
                    }
                }
                if (!paramEqual) {
                    continue;
                }
                // The two methods match;
                // this implies they have the same return type and parameter types,
                // which implies those have the same annotations (`type().equals(...)` considers annotations).
                om = i;
                // We still need to check method annotations and parameter annotations are equivalent.
                if (!compareAnnotations(method.declaredAnnotations(), om.declaredAnnotations())) {
                    return false;
                }
                for (int j = 0; j < method.parametersCount(); ++j) {
                    if (!compareAnnotations(method.parameters().get(j).declaredAnnotations(),
                            om.parameters().get(j).declaredAnnotations())) {
                        return false;
                    }
                }
            }
            //no further checks needed, we fully matched in the loop
            if (om == null) {
                return false;
            }
        }
        return true;
    }

    private static boolean compareAnnotations(Collection<AnnotationInstance> a, Collection<AnnotationInstance> b) {
        if (a.size() != b.size()) {
            return false;
        }
        Map<DotName, AnnotationInstance> lookup = b.stream()
                .collect(Collectors.toMap(AnnotationInstance::name, Function.identity()));

        for (AnnotationInstance i1 : a) {
            AnnotationInstance i2 = lookup.get(i1.name());
            if (i2 == null) {
                return false;
            }
            if (!compareAnnotation(i1, i2)) {
                return false;
            }
        }
        return true;
    }

    private static boolean compareAnnotation(AnnotationInstance a, AnnotationInstance b) {
        if (IGNORED_ANNOTATIONS.contains(a.name())) {
            return true;
        }
        List<AnnotationValue> valuesA = a.values();
        List<AnnotationValue> valuesB = b.values();
        if (valuesA.size() != valuesB.size()) {
            return false;
        }
        for (AnnotationValue valueA : valuesA) {
            AnnotationValue valueB = b.value(valueA.name());
            if (!valueA.equals(valueB)) {
                return false;
            }
        }
        return true;
    }
}
