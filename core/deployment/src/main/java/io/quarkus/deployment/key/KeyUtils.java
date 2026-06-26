package io.quarkus.deployment.key;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.quarkus.builder.item.BuildItem;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.annotations.Key;

/**
 * Utility for introspecting and filtering {@link Key @Key}-annotated fields.
 */
public final class KeyUtils {

    private KeyUtils() {
    }

    /**
     * Find the single {@code @Key}-annotated field on the given class, if any.
     *
     * @param clazz the class to inspect
     * @return the key descriptor, or {@code null} if no {@code @Key} field is found
     * @throws IllegalArgumentException if multiple {@code @Key} fields are found,
     *         or if the annotated field is not of type {@code String}
     */
    public static KeyDescriptor findKeyField(Class<?> clazz) {
        KeyDescriptor found = null;
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                Key keyAnn = field.getAnnotation(Key.class);
                if (keyAnn == null) {
                    continue;
                }
                if (found != null) {
                    throw new IllegalArgumentException(
                            "Multiple @Key fields found on " + clazz.getName()
                                    + ": " + found.field().getName() + " and " + field.getName()
                                    + ". Only one @Key field per class is supported.");
                }
                if (field.getType() != String.class) {
                    throw new IllegalArgumentException(
                            "@Key field must be of type String: " + field.getName()
                                    + " on " + clazz.getName() + " is " + field.getType().getName());
                }
                if (!Modifier.isPublic(field.getModifiers())
                        || !Modifier.isPublic(field.getDeclaringClass().getModifiers())) {
                    field.setAccessible(true);
                }
                found = new KeyDescriptor(keyAnn.value(), field);
            }
        }
        if (found != null && BuildItem.class.isAssignableFrom(clazz) && !MultiBuildItem.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException(
                    "@Key is only supported on MultiBuildItem classes, but " + clazz.getName()
                            + " is not a MultiBuildItem");
        }
        return found;
    }

    /**
     * Collect unique key values from keyed multi build item lists.
     */
    public static Set<String> collectUniqueKeys(Object[] methodArgs, List<KeyedParam> keyedParams) {
        Set<String> uniqueKeys = new LinkedHashSet<>();
        for (KeyedParam kp : keyedParams) {
            List<?> fullList = (List<?>) methodArgs[kp.index()];
            for (Object item : fullList) {
                String keyValue = kp.descriptor().getValue(item);
                if (keyValue != null) {
                    uniqueKeys.add(keyValue);
                }
            }
        }
        return uniqueKeys;
    }

    /**
     * Build a per-key copy of method arguments: sets the key parameter and filters keyed multi lists.
     * For singular keyed parameters (bare {@code MultiBuildItem}, not {@code List<>}), the filtered list
     * is unwrapped to a single item.
     */
    public static Object[] filterKeyedArgs(Object[] methodArgs, int keyParamIndex,
            List<KeyedParam> keyedParams, String keyValue) {
        Object[] perKeyArgs = methodArgs.clone();
        perKeyArgs[keyParamIndex] = keyValue;
        for (KeyedParam kp : keyedParams) {
            int paramIdx = kp.index();
            List<?> fullList = (List<?>) methodArgs[paramIdx];
            List<Object> filtered = new ArrayList<>();
            for (Object item : fullList) {
                if (keyValue.equals(kp.descriptor().getValue(item))) {
                    filtered.add(item);
                }
            }
            if (kp.singular()) {
                perKeyArgs[paramIdx] = filtered.isEmpty() ? null : filtered.get(0);
            } else {
                perKeyArgs[paramIdx] = Collections.unmodifiableList(filtered);
            }
        }
        return perKeyArgs;
    }
}
