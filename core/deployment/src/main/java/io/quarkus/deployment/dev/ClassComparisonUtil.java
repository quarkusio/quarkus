package io.quarkus.deployment.dev;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

public class ClassComparisonUtil {
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
        if (!compareAnnotations(clazz.classAnnotations(), old.classAnnotations())) {
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
            if (!compareAnnotations(of.annotations(), field.annotations())) {
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
                if (i.parameters().size() != method.parameters().size()) {
                    continue;
                }
                if (i.flags() != method.flags()) {
                    continue;
                }
                if (!Objects.equals(i.defaultValue(), method.defaultValue())) {
                    continue;
                }
                boolean paramEqual = true;
                for (int j = 0; j < method.parameters().size(); ++j) {
                    Type a = method.parameters().get(j);
                    Type b = i.parameters().get(j);
                    if (!a.equals(b)) {
                        paramEqual = false;
                        break;
                    }
                }
                if (!paramEqual) {
                    continue;
                }
                if (!compareMethodAnnotations(i.annotations(), method.annotations())) {
                    continue;
                }
                om = i;
            }
            //no further checks needed, we fully matched in the loop
            if (om == null) {
                return false;
            }
        }
        return true;
    }

    static boolean compareAnnotations(Collection<AnnotationInstance> a, Collection<AnnotationInstance> b) {
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

    static boolean compareMethodAnnotations(Collection<AnnotationInstance> a, Collection<AnnotationInstance> b) {
        if (a.size() != b.size()) {
            return false;
        }
        List<AnnotationInstance> method1 = new ArrayList<>();
        Map<Integer, List<AnnotationInstance>> params1 = new HashMap<>();
        methodMap(a, method1, params1);
        List<AnnotationInstance> method2 = new ArrayList<>();
        Map<Integer, List<AnnotationInstance>> params2 = new HashMap<>();
        methodMap(b, method2, params2);
        if (!compareAnnotations(method1, method2)) {
            return false;
        }
        if (!params1.keySet().equals(params2.keySet())) {
            return false;
        }
        for (Map.Entry<Integer, List<AnnotationInstance>> entry : params1.entrySet()) {
            List<AnnotationInstance> other = params2.get(entry.getKey());
            if (!compareAnnotations(other, entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    private static void methodMap(Collection<AnnotationInstance> b, List<AnnotationInstance> method2,
            Map<Integer, List<AnnotationInstance>> params2) {
        for (AnnotationInstance i : b) {
            if (i.target().kind() == AnnotationTarget.Kind.METHOD) {
                method2.add(i);
            } else {
                int index = i.target().asMethodParameter().position();
                List<AnnotationInstance> instances = params2.get(index);
                if (instances == null) {
                    params2.put(index, instances = new ArrayList<>());
                }
                instances.add(i);
            }
        }
    }

    private static boolean compareAnnotation(AnnotationInstance a, AnnotationInstance b) {
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
