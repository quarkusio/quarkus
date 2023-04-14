package io.quarkus.observability.common.config;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

public class ContainerConfigUtil {
    /**
     * We need a per config method equals,
     * so that we know when the config changes.
     */
    public static boolean isEqual(ContainerConfig cc1, ContainerConfig cc2) {
        Class<?> c1 = cc1.getClass();
        Class<?> c2 = cc1.getClass();
        if (!c1.equals(c2)) {
            return false;
        }

        Class<?> i = Arrays.stream(c1.getInterfaces())
                .filter(ContainerConfig.class::isAssignableFrom)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Missing ContainerConfig based interface"));
        Method[] methods = i.getMethods(); // should get all config methods
        for (Method m : methods) {
            Object v1 = invoke(m, cc1);
            Object v2 = invoke(m, cc2);
            if (!Objects.equals(v1, v2)) {
                return false;
            }
        }
        return true;
    }

    private static Object invoke(Method m, Object target) {
        try {
            return m.invoke(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
