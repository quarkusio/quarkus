package io.quarkus.resteasy.jsonb.deployment;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;

public final class PropertyUtil {

    private PropertyUtil() {
    }

    private static final String IS_PREFIX = "is";
    private static final String GET_PREFIX = "get";

    /**
     * @return a Map where the keys are the getters and the values are the corresponding fields
     */
    static Map<MethodInfo, FieldInfo> getGetterMethods(ClassInfo classInfo) {
        List<MethodInfo> allMethods = classInfo.methods();
        Map<MethodInfo, FieldInfo> result = new HashMap<>();
        for (MethodInfo method : allMethods) {
            if (isGetter(method)) {
                result.put(method, classInfo.field(toFieldName(method)));
            }
        }
        return result;
    }

    static List<FieldInfo> getPublicFieldsWithoutGetters(ClassInfo classInfo, Collection<MethodInfo> getters) {
        Set<String> getterPropertyNames = new HashSet<>();
        for (MethodInfo getter : getters) {
            getterPropertyNames.add(toFieldName(getter));
        }
        List<FieldInfo> allFields = classInfo.fields();
        List<FieldInfo> result = new ArrayList<>(allFields.size());
        for (FieldInfo field : allFields) {
            if (Modifier.isPublic(field.flags()) && !getterPropertyNames.contains(field.name())) {
                result.add(field);
            }
        }
        return result;
    }

    private static boolean isGetter(MethodInfo m) {
        return (m.name().startsWith(GET_PREFIX) || m.name().startsWith(IS_PREFIX)) && m.parameters().size() == 0;
    }

    /**
     * Returns the corresponding property name
     * Assumes that the input is a getter
     */
    public static String toFieldName(MethodInfo getter) {
        String name = getter.name();
        return lowerFirstLetter(name.substring(name.startsWith(IS_PREFIX) ? 2 : 3));
    }

    private static String lowerFirstLetter(String name) {
        if (name.length() == 0) {
            //methods named get() or set()
            return name;
        }
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1)) &&
                Character.isUpperCase(name.charAt(0))) {
            return name;
        }
        char[] chars = name.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }
}
