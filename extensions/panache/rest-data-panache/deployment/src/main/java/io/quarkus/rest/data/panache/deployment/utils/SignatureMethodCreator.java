package io.quarkus.rest.data.panache.deployment.utils;

import static io.quarkus.gizmo.Type.classType;
import static io.quarkus.gizmo.Type.parameterizedType;
import static io.quarkus.rest.data.panache.deployment.utils.TypeUtils.toGizmoType;

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.core.Response;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.SignatureBuilder;
import io.quarkus.gizmo.Type;
import io.smallrye.mutiny.Uni;

public final class SignatureMethodCreator {

    private static final Type RESPONSE_TYPE = Type.classType(Response.class);

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
            Parameter... parameters) {
        List<Type> paramTypes = new ArrayList<>();
        List<String> paramNames = new ArrayList<>();
        List<Object> paramClasses = new ArrayList<>();
        for (Parameter param : parameters) {
            paramNames.add(param.name);
            paramTypes.add(param.type);
            paramClasses.add(param.clazz);
        }

        MethodCreator methodCreator = classCreator.getMethodCreator(methodName, returnType.classType,
                paramClasses.toArray(new Object[0]));
        SignatureBuilder.MethodSignatureBuilder signatureBuilder = SignatureBuilder.forMethod()
                .setReturnType(returnType.type);
        paramTypes.forEach(signatureBuilder::addParameterType);
        methodCreator.setSignature(signatureBuilder.build());
        methodCreator.setParameterNames(paramNames.toArray(new String[0]));

        return methodCreator;
    }

    public static Parameter param(String name, Object type) {
        return param(name, type, toGizmoType(type));
    }

    public static Parameter param(String name, Object clazz, Type type) {
        Parameter parameter = new Parameter();
        parameter.name = name;
        parameter.clazz = clazz;
        parameter.type = type;

        return parameter;
    }

    public static class Parameter {
        private String name;
        private Type type;
        private Object clazz;

        public String getName() {
            return name;
        }

        public Type getType() {
            return type;
        }

        public Object getClazz() {
            return clazz;
        }
    }

    public static class ReturnType {
        private Class<?> classType;
        private Type type;
    }

    public static ReturnType responseType() {
        ReturnType returnType = new ReturnType();
        returnType.classType = Response.class;
        returnType.type = RESPONSE_TYPE;
        return returnType;
    }

    public static ReturnType uniType(Object... arguments) {
        ReturnType returnType = new ReturnType();
        Type[] typeArguments = new Type[arguments.length];
        for (int index = 0; index < arguments.length; index++) {
            typeArguments[index] = toGizmoType(arguments[index]);
        }

        returnType.classType = Uni.class;
        returnType.type = parameterizedType(classType(Uni.class), typeArguments);
        return returnType;
    }
}
