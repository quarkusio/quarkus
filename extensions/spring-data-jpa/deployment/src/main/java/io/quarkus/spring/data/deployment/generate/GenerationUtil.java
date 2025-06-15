package io.quarkus.spring.data.deployment.generate;

import static io.quarkus.spring.data.deployment.DotNames.JPA_NAMED_QUERIES;
import static io.quarkus.spring.data.deployment.DotNames.JPA_NAMED_QUERY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
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

    static List<DotName> extendedSpringDataRepos(ClassInfo repositoryToImplement, IndexView index) {
        List<DotName> result = new ArrayList<>();
        for (DotName interfaceName : repositoryToImplement.interfaceNames()) {
            if (DotNames.SUPPORTED_REPOSITORIES.contains(interfaceName)) {
                result.add(interfaceName);
            } else {
                result.addAll(extendedSpringDataRepos(index.getClassByName(interfaceName), index));
            }
        }
        return result;
    }

    static boolean isIntermediateRepository(DotName interfaceName, IndexView indexView) {
        if (DotNames.SUPPORTED_REPOSITORIES.contains(interfaceName)) {
            return false;
        }
        return !extendedSpringDataRepos(indexView.getClassByName(interfaceName), indexView).isEmpty();
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
        for (Type parameter : methodInfo.parameterTypes()) {
            parameterTypesStr.add(parameter.name().toString());
        }
        return MethodDescriptor.ofMethod(generatedClassName, methodInfo.name(),
                methodInfo.returnType().name().toString(), parameterTypesStr.toArray(new String[0]));
    }

    static AnnotationInstance getNamedQueryForMethod(MethodInfo methodInfo, ClassInfo entityClassInfo) {
        // try @NamedQuery
        AnnotationInstance namedQueryAnnotation = getNamedQueryAnnotationForMethod(methodInfo, entityClassInfo);
        if (namedQueryAnnotation != null) {
            return namedQueryAnnotation;
        }

        // try @NamedQueries
        return getNamedQueriesAnnotationForMethod(methodInfo, entityClassInfo);
    }

    private static AnnotationInstance getNamedQueryAnnotationForMethod(MethodInfo methodInfo,
            ClassInfo entityClassInfo) {
        String methodName = methodInfo.name();
        AnnotationInstance namedQueryAnnotation = entityClassInfo.declaredAnnotation(JPA_NAMED_QUERY);
        if (namedQueryAnnotation != null
                && isMethodDeclaredInNamedQuery(entityClassInfo, methodName, namedQueryAnnotation)) {
            return namedQueryAnnotation;
        }

        return null;
    }

    private static AnnotationInstance getNamedQueriesAnnotationForMethod(MethodInfo methodInfo,
            ClassInfo entityClassInfo) {
        String methodName = methodInfo.name();
        AnnotationInstance namedQueriesAnnotation = entityClassInfo.declaredAnnotation(JPA_NAMED_QUERIES);
        if (namedQueriesAnnotation != null) {
            for (AnnotationValue annotationInstanceValues : namedQueriesAnnotation.values()) {
                for (AnnotationInstance annotationInstance : annotationInstanceValues.asNestedArray()) {
                    if (isMethodDeclaredInNamedQuery(entityClassInfo, methodName, annotationInstance)) {
                        return annotationInstance;
                    }
                }
            }
        }

        return null;
    }

    private static boolean isMethodDeclaredInNamedQuery(ClassInfo entityClassInfo, String methodName,
            AnnotationInstance namedQuery) {
        AnnotationValue namedQueryName = namedQuery.value("name");
        if (namedQueryName == null) {
            return false;
        }

        return String.format("%s.%s", entityClassInfo.name().withoutPackagePrefix(), methodName)
                .equals(namedQueryName.value());
    }

}
