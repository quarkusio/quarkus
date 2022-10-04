package org.jboss.resteasy.reactive.server.processor.util;

import java.util.List;

import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;

public class KotlinUtils {

    private KotlinUtils() {
    }

    public static boolean isSuspendMethod(MethodInfo methodInfo) {
        List<Type> parameters = methodInfo.parameterTypes();
        if (parameters.isEmpty()) {
            return false;
        }

        return ResteasyReactiveDotNames.CONTINUATION.equals(parameters.get(parameters.size() - 1).name());
    }
}
