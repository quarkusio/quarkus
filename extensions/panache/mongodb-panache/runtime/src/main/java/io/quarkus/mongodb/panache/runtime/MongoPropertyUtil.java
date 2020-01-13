package io.quarkus.mongodb.panache.runtime;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.bson.codecs.pojo.annotations.BsonProperty;

final class MongoPropertyUtil {

    private MongoPropertyUtil() {
        //prevent initialization
    }

    static Map<String, String> extractReplacementMap(Class<?> clazz) {
        //TODO cache the replacement map or pre-compute it during build (using reflection or jandex)
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
    static String decapitalize(String name) {
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
