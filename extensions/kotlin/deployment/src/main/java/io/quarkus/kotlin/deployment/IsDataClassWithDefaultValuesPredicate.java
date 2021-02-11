package io.quarkus.kotlin.deployment;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.function.Predicate;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;

/**
 * Tests whether a class is a data class (based on this answer:
 * https://discuss.kotlinlang.org/t/detect-data-class-in-runtime/6155/2)
 * and whether the class has default values for fields (default values leads to having multiple constructors in bytecode)
 */
public class IsDataClassWithDefaultValuesPredicate implements Predicate<ClassInfo> {

    @Override
    public boolean test(ClassInfo classInfo) {
        int ctorCount = 0;
        boolean hasCopyMethod = false;
        boolean hasStaticCopyMethod = false;
        boolean hasComponent1Method = false;
        List<MethodInfo> methods = classInfo.methods();
        for (MethodInfo method : methods) {
            String methodName = method.name();
            if ("<init>".equals(methodName)) {
                ctorCount++;
            } else if ("component1".equals(methodName) && Modifier.isFinal(method.flags())) {
                hasComponent1Method = true;
            } else if ("copy".equals(methodName) && Modifier.isFinal(method.flags())) {
                hasCopyMethod = true;
            } else if ("copy$default".equals(methodName) && Modifier.isStatic(method.flags())) {
                hasStaticCopyMethod = true;
            }
        }
        return ctorCount > 1 && hasComponent1Method && hasCopyMethod && hasStaticCopyMethod;
    }
}
