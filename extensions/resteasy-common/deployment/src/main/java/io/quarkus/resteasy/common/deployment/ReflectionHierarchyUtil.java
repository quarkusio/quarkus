package io.quarkus.resteasy.common.deployment;

import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

public final class ReflectionHierarchyUtil {

    public static boolean isReflectionDeclarationRequiredFor(Type type) {
        DotName className = getClassName(type);
        if (className == null) {
            return false;
        }

        // for now limit this special parameterized type handling only to known use cases
        if (Type.Kind.PARAMETERIZED_TYPE.equals(type.kind()) && ResteasyDotNames.COMPLETION_STAGE.equals(className)) {
            // this solution is not perfect since returning true here will make ReflectiveHierarchyStep register all parameters
            // for reflection but extra reflection registration is big problem
            ParameterizedType parameterizedType = type.asParameterizedType();
            boolean required = false;
            for (Type argumentType : parameterizedType.arguments()) {
                if (isReflectionDeclarationRequiredFor(argumentType)) {
                    required = true;
                    break;
                }
            }
            return required;
        } else {
            return !ResteasyDotNames.TYPES_IGNORED_FOR_REFLECTION.contains(className);
        }
    }

    private static DotName getClassName(Type type) {
        switch (type.kind()) {
            case CLASS:
            case PARAMETERIZED_TYPE:
                return type.name();
            case ARRAY:
                return getClassName(type.asArrayType().component());
            default:
                return null;
        }
    }
}
