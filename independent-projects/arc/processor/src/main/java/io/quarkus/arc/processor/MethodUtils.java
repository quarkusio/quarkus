package io.quarkus.arc.processor;

import java.util.Collection;
import java.util.List;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 17/07/2019
 */
public class MethodUtils {

    public static boolean isOverriden(MethodInfo method, Collection<MethodInfo> previousMethods) {
        for (MethodInfo other : previousMethods) {
            if (matchesSignature(method, other)) {
                return true;
            }
        }

        return false;
    }

    private static boolean matchesSignature(MethodInfo method, MethodInfo subclassMethod) {
        if (!method.name().equals(subclassMethod.name())) {
            return false;
        }
        List<Type> parameters = method.parameters();
        List<Type> subParameters = subclassMethod.parameters();

        int paramCount = parameters.size();
        if (paramCount != subParameters.size()) {
            return false;
        }

        if (paramCount == 0) {
            return true;
        }

        for (int i = 0; i < paramCount; i++) {
            if (!isTypeEqual(parameters.get(i), subParameters.get(i))) {
                return false;
            }
        }

        return true;
    }

    private static boolean isTypeEqual(Type a, Type b) {
        return toRawType(a).equals(toRawType(b));
    }

    private static DotName toRawType(Type a) {
        switch (a.kind()) {
            case CLASS:
            case PRIMITIVE:
            case ARRAY:
                return a.name();
            case PARAMETERIZED_TYPE:
                return a.asParameterizedType().name();
            case TYPE_VARIABLE:
            case UNRESOLVED_TYPE_VARIABLE:
            case WILDCARD_TYPE:
            default:
                return DotNames.OBJECT;
        }
    }

    private MethodUtils() {
    }
}
