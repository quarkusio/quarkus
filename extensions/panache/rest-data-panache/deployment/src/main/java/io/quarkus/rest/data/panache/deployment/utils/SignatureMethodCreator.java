package io.quarkus.rest.data.panache.deployment.utils;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.gizmo.MethodCreator;

public final class SignatureMethodCreator {

    /**
     * Creates a method using a signature (which allows declaring parameterized types like lists).
     *
     * For example, for the method: "List<String> list(List<String> sort, int size, int other, String uri)"
     * It will use the following signature the generated the method:
     * "(Ljava/util/List<Ljava/lang/String;>;IILjava/lang/String;)Ljava/util/List<Ljava/lang/String;>;".
     *
     * One useful utility to verify the method signatures is using "javap -v Test.class" where the Test java class is a compiled
     * version of the method you want to see the signature.
     */
    public static MethodCreator getMethodCreator(String methodName, ClassCreator classCreator, ReturnType returnType,
            Object... parameters) {
        MethodCreator methodCreator = classCreator.getMethodCreator(methodName, returnType.type, parameters);

        StringBuilder signatureBuilder = new StringBuilder();
        // Params first within parenthesis. If method is: "void method(Integer a)", it should return "(Ljava/lang/Integer;)":
        signatureBuilder.append("(");
        parametersToSignature(signatureBuilder, parameters);

        signatureBuilder.append(")");

        // Then, result type. If method is: "List<String> method(Integer a)",
        // it should return "Ljava/util/List<Ljava/lang/String;>;"
        signatureBuilder.append(stringToSignature(returnType.type.getName()));
        if (returnType.parameterTypes.length > 0) {
            signatureBuilder.append("<");
            parametersToSignature(signatureBuilder, returnType.parameterTypes);
            signatureBuilder.append(">");
        }

        signatureBuilder.append(";");

        methodCreator.setSignature(signatureBuilder.toString());

        return methodCreator;
    }

    private static void parametersToSignature(StringBuilder signatureBuilder, Object[] parameters) {
        for (Object parameter : parameters) {
            if (parameter instanceof Class) {
                signatureBuilder.append(DescriptorUtils.classToStringRepresentation((Class<?>) parameter));
            } else if (parameter instanceof String) {
                signatureBuilder.append(stringToSignature((String) parameter) + ";");
            }
        }
    }

    private static String stringToSignature(String className) {
        return "L" + className.replace('.', '/');
    }

    public static ReturnType ofType(Class<?> type, Object... parameterTypes) {
        ReturnType returnType = new ReturnType();
        returnType.type = type;
        returnType.parameterTypes = parameterTypes;
        return returnType;
    }

    public static class ReturnType {
        private Class<?> type;
        private Object[] parameterTypes;
    }
}
