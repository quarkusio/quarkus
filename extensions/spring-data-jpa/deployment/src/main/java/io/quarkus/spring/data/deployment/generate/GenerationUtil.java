package io.quarkus.spring.data.deployment.generate;

import static io.quarkus.spring.data.deployment.DotNames.JPA_NAMED_QUERIES;
import static io.quarkus.spring.data.deployment.DotNames.JPA_NAMED_QUERY;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.gizmo2.desc.ClassMethodDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
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
        Set<MethodInfo> result = new LinkedHashSet<>();
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

    /**
     * Build a method key string for tracking existing methods in a Set.
     */
    static String methodKey(String name, String returnType, String... paramTypes) {
        return name + "(" + String.join(",", paramTypes) + ")" + returnType;
    }

    /**
     * Build a method key string from a MethodInfo for a given generated class.
     */
    static String methodKey(String generatedClassName, MethodInfo methodInfo) {
        final List<String> parameterTypesStr = new ArrayList<>();
        for (Type parameter : methodInfo.parameterTypes()) {
            parameterTypesStr.add(parameter.name().toString());
        }
        return methodKey(methodInfo.name(), methodInfo.returnType().name().toString(),
                parameterTypesStr.toArray(new String[0]));
    }

    /**
     * Create a MethodTypeDesc from the return type and parameter types.
     */
    static MethodTypeDesc toMethodTypeDesc(String returnType, String... paramTypes) {
        ClassDesc retDesc = toClassDesc(returnType);
        ClassDesc[] paramDescs = new ClassDesc[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            paramDescs[i] = toClassDesc(paramTypes[i]);
        }
        return MethodTypeDesc.of(retDesc, paramDescs);
    }

    /**
     * Convert a dot-separated class name or primitive type name to a ClassDesc.
     * Also handles JVM array type descriptors like {@code [Ljava.lang.Object;} and
     * dot-name array forms like {@code java.lang.Object[]}.
     */
    static ClassDesc toClassDesc(String typeName) {
        // Handle JVM internal array descriptors (e.g. "[Ljava.lang.Object;", "[[I")
        if (typeName.startsWith("[")) {
            return ClassDesc.ofDescriptor(typeName.replace('.', '/'));
        }
        // Handle dot-name array forms (e.g. "java.lang.Object[]")
        if (typeName.endsWith("[]")) {
            String componentType = typeName.substring(0, typeName.length() - 2);
            return toClassDesc(componentType).arrayType();
        }
        return switch (typeName) {
            case "void" -> ConstantDescs.CD_void;
            case "boolean" -> ConstantDescs.CD_boolean;
            case "byte" -> ConstantDescs.CD_byte;
            case "short" -> ConstantDescs.CD_short;
            case "int" -> ConstantDescs.CD_int;
            case "long" -> ConstantDescs.CD_long;
            case "float" -> ConstantDescs.CD_float;
            case "double" -> ConstantDescs.CD_double;
            case "char" -> ConstantDescs.CD_char;
            default -> ClassDesc.of(typeName);
        };
    }

    /**
     * Build a MethodDesc for a method in a generated class from a MethodInfo.
     * Used in case where we can't simply use the declaring class from MethodInfo.
     */
    static MethodDesc toMethodDesc(String generatedClassName, MethodInfo methodInfo) {
        final List<String> parameterTypesStr = new ArrayList<>();
        for (Type parameter : methodInfo.parameterTypes()) {
            parameterTypesStr.add(parameter.name().toString());
        }
        String returnType = methodInfo.returnType().name().toString();
        MethodTypeDesc mtd = toMethodTypeDesc(returnType, parameterTypesStr.toArray(new String[0]));
        return ClassMethodDesc.of(ClassDesc.of(generatedClassName), methodInfo.name(), mtd);
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

    private static AnnotationInstance getNamedQueryAnnotationForMethod(MethodInfo methodInfo, ClassInfo entityClassInfo) {
        String methodName = methodInfo.name();
        AnnotationInstance namedQueryAnnotation = entityClassInfo.declaredAnnotation(JPA_NAMED_QUERY);
        if (namedQueryAnnotation != null && isMethodDeclaredInNamedQuery(entityClassInfo, methodName, namedQueryAnnotation)) {
            return namedQueryAnnotation;
        }

        return null;
    }

    private static AnnotationInstance getNamedQueriesAnnotationForMethod(MethodInfo methodInfo, ClassInfo entityClassInfo) {
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

        return String.format("%s.%s", entityClassInfo.name().withoutPackagePrefix(), methodName).equals(namedQueryName.value());
    }

}
