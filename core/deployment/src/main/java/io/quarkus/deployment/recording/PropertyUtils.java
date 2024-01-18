
package io.quarkus.deployment.recording;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

final class PropertyUtils {

    static final ConcurrentMap<Class<?>, Property[]> CACHE = new ConcurrentHashMap<>();

    private static final Function<Class<?>, Property[]> FUNCTION = new Function<Class<?>, Property[]>() {
        @Override
        public Property[] apply(Class<?> type) {
            if (type.isRecord()) {
                RecordComponent[] recordComponents = type.getRecordComponents();
                return Arrays.stream(recordComponents)
                        .map(rc -> new Property(rc.getName(), rc.getAccessor(), null, rc.getType())).toArray(Property[]::new);
            }

            List<Property> ret = new ArrayList<>();
            Method[] methods = type.getMethods();

            Map<String, Method> getters = new HashMap<>();
            Map<String, Method> isGetters = new HashMap<>();
            Map<String, Method> setters = new HashMap<>();
            for (Method i : methods) {
                if (i.getName().startsWith("get") && i.getName().length() > 3 && i.getParameterCount() == 0
                        && i.getReturnType() != void.class) {
                    String name = Character.toLowerCase(i.getName().charAt(3)) + i.getName().substring(4);
                    Method existingGetter = getters.get(name);
                    // In some cases the overridden methods from supertypes can also appear in the array (for some reason).
                    // We want the most specific methods.
                    if (existingGetter == null || existingGetter.getReturnType().isAssignableFrom(i.getReturnType())) {
                        getters.put(name, i);
                    }
                } else if (i.getName().startsWith("is") && i.getName().length() > 3 && i.getParameterCount() == 0
                        && (i.getReturnType() == boolean.class || i.getReturnType() == Boolean.class)) {
                    String name = Character.toLowerCase(i.getName().charAt(2)) + i.getName().substring(3);
                    isGetters.put(name, i);
                } else if (i.getName().startsWith("set") && i.getName().length() > 3 && i.getParameterCount() == 1) {
                    String name = Character.toLowerCase(i.getName().charAt(3)) + i.getName().substring(4);
                    setters.put(name, i);
                }
            }

            Set<String> names = new HashSet<>(getters.keySet());
            names.addAll(isGetters.keySet());
            names.addAll(setters.keySet());
            for (String i : names) {
                Method get = getters.get(i);
                if (get == null) {
                    get = isGetters.get(i); // If there is no "get" getter, use the "is" getter
                }
                Method set = setters.get(i);
                if (get == null) {
                    ret.add(new Property(i, get, set, set.getParameterTypes()[0]));
                } else if (set == null) {
                    ret.add(new Property(i, get, set, get.getReturnType()));
                } else {
                    Class<?> gt = get.getReturnType();
                    Class<?> st = set.getParameterTypes()[0];
                    if (gt == st) {
                        ret.add(new Property(i, get, set, gt));
                    } else if (gt.isAssignableFrom(st)) {
                        ret.add(new Property(i, get, set, gt));
                    } else if (st.isAssignableFrom(gt)) {
                        ret.add(new Property(i, get, set, st));
                    }
                }
            }

            return ret.toArray(new Property[ret.size()]);
        }
    };

    public static Property[] getPropertyDescriptors(Object param) {
        return CACHE.computeIfAbsent(param.getClass(), FUNCTION);
    }

    public static class Property {
        final String name;
        final Method readMethod;
        final Method writeMethod;
        final Class<?> propertyType;

        public Property(String name, Method readMethod, Method writeMethod, Class<?> propertyType) {
            this.name = name;
            this.readMethod = readMethod;
            this.writeMethod = writeMethod;
            this.propertyType = propertyType;
        }

        public String getName() {
            return name;
        }

        public Method getReadMethod() {
            return readMethod;
        }

        public Method getWriteMethod() {
            return writeMethod;
        }

        public Class<?> getPropertyType() {
            return propertyType;
        }

        public Class<?> getDeclaringClass() {
            return readMethod != null ? readMethod.getDeclaringClass() : writeMethod.getDeclaringClass();
        }

        public Object read(Object target) {
            try {
                return readMethod.invoke(target);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public void write(Object target, Object value) {
            try {
                writeMethod.invoke(target, value);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
