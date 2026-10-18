package io.quarkus.mongodb.panache.common.runtime;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bson.codecs.pojo.annotations.BsonProperty;
import org.jboss.logging.Logger;

public final class MongoPropertyUtil {
    private static final Logger LOGGER = Logger.getLogger(MongoPropertyUtil.class);

    // will be replaced at augmentation phase
    private static volatile Map<String, Map<String, String>> replacementCache = Collections.emptyMap();

    private MongoPropertyUtil() {
        //prevent initialization
    }

    public static Set<String> collectFields(Class<?> type) {
        Set<String> fieldNames = new HashSet<>();
        // gather field names from getters
        for (Method method : type.getMethods()) {
            if (method.getName().startsWith("get") && !method.getName().equals("getClass")) {
                String fieldName = MongoPropertyUtil.decapitalize(method.getName().substring(3));
                fieldNames.add(fieldName);
            }
        }

        // gather field names from public fields
        for (Field field : type.getFields()) {
            fieldNames.add(field.getName());
        }

        // replace fields that have @BsonProperty mappings
        Map<String, String> replacementMap = MongoPropertyUtil.getReplacementMap(type);
        for (Map.Entry<String, String> entry : replacementMap.entrySet()) {
            if (fieldNames.contains(entry.getKey())) {
                fieldNames.remove(entry.getKey());
                fieldNames.add(entry.getValue());
            }
        }
        return fieldNames;
    }

    public static void setReplacementCache(Map<String, Map<String, String>> newReplacementCache) {
        replacementCache = newReplacementCache;
    }

    public static Map<String, String> getReplacementMap(Class<?> clazz) {
        return replacementCache.computeIfAbsent(clazz.getName(), s -> buildWithReflection(clazz));
    }

    private static Map<String, String> buildWithReflection(Class<?> clazz) {
        LOGGER.info("No replacement map found for " + clazz.getName()
                + ", default to using reflection. To avoid that, make sure the class is in the Jandex index or, " +
                "if using class based projection, annotated it with @ProjectionFor");
        Map<String, String> replacementMap = new HashMap<>();
        for (Field field : clazz.getDeclaredFields()) {
            BsonProperty bsonProperty = field.getAnnotation(BsonProperty.class);
            if (bsonProperty != null) {
                replacementMap.put(field.getName(), bsonProperty.value());
            }
        }
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().startsWith("get")) {
                // we try to replace also for getter
                BsonProperty bsonProperty = method.getAnnotation(BsonProperty.class);
                if (bsonProperty != null) {
                    String fieldName = decapitalize(method.getName().substring(3));
                    replacementMap.put(fieldName, bsonProperty.value());
                }
            }
        }
        return replacementMap;
    }

    // copied from JavaBeanUtil that is inside the core deployment module so not accessible at runtime.
    // See conventions expressed by https://docs.oracle.com/javase/7/docs/api/java/beans/Introspector.html#decapitalize(java.lang.String)
    private static String decapitalize(String name) {
        if (name != null && name.length() != 0) {
            if (name.length() > 1 && Character.isUpperCase(name.charAt(1))) {
                return name;
            } else {
                char[] chars = name.toCharArray();
                chars[0] = Character.toLowerCase(chars[0]);
                return new String(chars);
            }
        } else {
            return name;
        }
    }
}
