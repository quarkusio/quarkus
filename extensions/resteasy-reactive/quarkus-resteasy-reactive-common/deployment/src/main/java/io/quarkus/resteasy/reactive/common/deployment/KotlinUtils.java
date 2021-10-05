package io.quarkus.resteasy.reactive.common.deployment;

import static io.quarkus.resteasy.reactive.common.deployment.QuarkusResteasyReactiveDotNames.CONTINUATION;

import java.util.List;

import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

public class KotlinUtils {

    private KotlinUtils() {
    }

    public static boolean isSuspendMethod(MethodInfo methodInfo) {
        List<Type> parameters = methodInfo.parameters();
        if (parameters.isEmpty()) {
            return false;
        }

        return CONTINUATION.equals(parameters.get(parameters.size() - 1).name());
    }
}
