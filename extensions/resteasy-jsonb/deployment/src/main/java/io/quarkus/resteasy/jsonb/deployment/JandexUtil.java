package io.quarkus.resteasy.jsonb.deployment;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

public final class JandexUtil {

    private JandexUtil() {
    }

    // includes annotations on superclasses, all interfaces and package-info
    public static Map<DotName, AnnotationInstance> getEffectiveClassAnnotations(DotName classDotName, IndexView index) {
        Map<DotName, AnnotationInstance> result = new HashMap<>();
        getEffectiveClassAnnotationsRec(classDotName, index, result);

        // we need these because they could contain jsonb annotations that alter the default behavior for all
        // classes in the package
        Collection<AnnotationInstance> annotationsFromPackage = getAnnotationsOfPackage(classDotName, index);
        if (!annotationsFromPackage.isEmpty()) {
            for (AnnotationInstance packageAnnotation : annotationsFromPackage) {
                if (!result.containsKey(packageAnnotation.name())) {
                    result.put(packageAnnotation.name(), packageAnnotation);
                }
            }
        }
        return result;
    }

    private static void getEffectiveClassAnnotationsRec(DotName classDotName, IndexView index,
            Map<DotName, AnnotationInstance> collected) {
        // annotations previously collected have higher "priority" so we need to make sure we don't add them again
        ClassInfo classInfo = index.getClassByName(classDotName);
        if (classInfo == null) {
            return;
        }

        Collection<AnnotationInstance> newInstances = classInfo.classAnnotations();
        for (AnnotationInstance newInstance : newInstances) {
            if (!collected.containsKey(newInstance.name())) {
                collected.put(newInstance.name(), newInstance);
            }
        }

        // collect annotations from the super type until we reach object
        if (!DotNames.OBJECT.equals(classInfo.superName())) {
            getEffectiveClassAnnotationsRec(classInfo.superName(), index, collected);
        }

        // collect annotations from all interfaces
        for (DotName interfaceDotName : classInfo.interfaceNames()) {
            getEffectiveClassAnnotationsRec(interfaceDotName, index, collected);
        }
    }

    private static Collection<AnnotationInstance> getAnnotationsOfPackage(DotName classDotName, IndexView index) {
        String className = classDotName.toString();
        if (!className.contains(".")) {
            return Collections.emptyList();
        }
        int i = className.lastIndexOf('.');
        String packageName = className.substring(0, i);
        ClassInfo packageClassInfo = index.getClassByName(DotName.createSimple(packageName + ".package-info"));
        if (packageClassInfo == null) {
            return Collections.emptyList();
        }

        return packageClassInfo.classAnnotations();
    }

    // determine whether the class contains any interface (however far up the tree) that contains a default method
    public static boolean containsInterfacesWithDefaultMethods(ClassInfo classInfo, IndexView index) {
        List<DotName> interfaceNames = classInfo.interfaceNames();
        for (DotName interfaceName : interfaceNames) {
            ClassInfo interfaceClassInfo = index.getClassByName(interfaceName);
            if (interfaceClassInfo == null) {
                continue;
            }
            final List<MethodInfo> methods = interfaceClassInfo.methods();
            for (MethodInfo method : methods) {
                // essentially the same as java.lang.reflect.Method#isDefault
                if (((method.flags() & (Modifier.ABSTRACT | Modifier.PUBLIC | Modifier.STATIC)) == Modifier.PUBLIC)) {
                    return true;
                }
            }
            return containsInterfacesWithDefaultMethods(interfaceClassInfo, index);
        }
        return false;
    }

}
