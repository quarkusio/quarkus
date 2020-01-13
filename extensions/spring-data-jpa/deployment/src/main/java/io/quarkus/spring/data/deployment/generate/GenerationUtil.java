package io.quarkus.spring.data.deployment.generate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.spring.data.deployment.DotNames;

public final class GenerationUtil {

    private GenerationUtil() {
    }

    static List<DotName> extendedSpringDataRepos(ClassInfo repositoryToImplement) {
        List<DotName> result = new ArrayList<>();
        for (DotName interfaceName : repositoryToImplement.interfaceNames()) {
            if (DotNames.SUPPORTED_REPOSITORIES.contains(interfaceName)) {
                result.add(interfaceName);
            }
        }
        return result;
    }

    static Set<MethodInfo> interfaceMethods(Collection<DotName> interfaces, IndexView index) {
        Set<MethodInfo> result = new HashSet<>();
        for (DotName dotName : interfaces) {
            ClassInfo classInfo = index.getClassByName(dotName);
            result.addAll(classInfo.methods());
            List<DotName> extendedInterfaces = classInfo.interfaceNames();
            if (!extendedInterfaces.isEmpty()) {
                result.addAll(interfaceMethods(extendedInterfaces, index));
            }
        }
        return result;
    }

    // Used in case where we can't simply use MethodDescriptor.of(MethodInfo)
    // because that used the class of the method
    static MethodDescriptor toMethodDescriptor(String generatedClassName, MethodInfo methodInfo) {
        final List<String> parameterTypesStr = new ArrayList<>();
        for (Type parameter : methodInfo.parameters()) {
            parameterTypesStr.add(parameter.name().toString());
        }
        return MethodDescriptor.ofMethod(generatedClassName, methodInfo.name(), methodInfo.returnType().name().toString(),
                parameterTypesStr.toArray(new String[0]));
    }

}
