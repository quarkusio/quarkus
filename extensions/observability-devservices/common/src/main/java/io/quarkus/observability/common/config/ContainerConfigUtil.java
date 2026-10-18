package io.quarkus.observability.common.config;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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

        Method[] methods = getMethods(c1);
        for (Method m : methods) {
            Object v1 = invoke(m, cc1);
            Object v2 = invoke(m, cc2);
            if (!Objects.equals(v1, v2)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get all properties to override from container config instance.
     *
     * @param config the container config
     * @return map of properties to override
     */
    public static Map<String, Object> propertiesToOverride(ContainerConfig config) {
        Map<String, Object> map = new HashMap<>();
        for (Method m : getMethods(config.getClass())) {
            OverrideProperty override = m.getAnnotation(OverrideProperty.class);
            if (override != null) {
                String key = override.value();
                Object value = invoke(m, config);
                if (value instanceof Optional<?>) {
                    Optional<?> optional = (Optional<?>) value;
                    optional.ifPresent(o -> map.put(key, o));
                } else if (value != null) {
                    map.put(key, value);
                }
            }
        }
        return map;
    }

    private static Method[] getMethods(Class<?> c1) {
        Class<?> i = Arrays.stream(c1.getInterfaces())
                .filter(ContainerConfig.class::isAssignableFrom)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Missing ContainerConfig based interface"));
        return i.getMethods();
    }

    private static Object invoke(Method m, Object target) {
        try {
            return m.invoke(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
