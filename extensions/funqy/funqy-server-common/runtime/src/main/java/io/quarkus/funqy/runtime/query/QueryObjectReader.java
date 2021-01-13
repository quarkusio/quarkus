package io.quarkus.funqy.runtime.query;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Turn URI parameter map into an object
 *
 */
class QueryObjectReader extends BaseObjectReader {

    Map<String, ValueSetter> properties = new HashMap<>();
    Class clz;

    @Override
    public Object create() {
        try {
            return clz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    QueryObjectReader(Class clz, QueryObjectMapper mapper) {
        this.clz = clz;
        for (Method m : clz.getMethods()) {
            if (!isSetter(m))
                continue;
            Class paramType = m.getParameterTypes()[0];
            Type paramGenericType = m.getGenericParameterTypes()[0];
            final Function<String, Object> extractor = mapper.extractor(paramType);

            String name;
            if (m.getName().length() > 4) {
                name = Character.toLowerCase(m.getName().charAt(3)) + m.getName().substring(4);
            } else {
                name = m.getName().substring(3).toLowerCase();
            }
            ValueSetter setter = new ValueSetter() {
                @Override
                public void setValue(Object target, String propName, Object value) {
                    try {
                        m.invoke(target, value);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public Function<String, Object> getExtractor() {
                    return extractor;
                }

                @Override
                public QueryPropertySetter getSetter() {
                    if (extractor == null) {
                        return mapper.setterFor(paramType, paramGenericType);
                    } else {
                        return null;
                    }
                }
            };
            properties.put(name, setter);
        }

    }

    static boolean isSetter(Method m) {
        return !Modifier.isStatic(m.getModifiers()) && m.getName().startsWith("set") && m.getName().length() > "set".length()
                && m.getParameterCount() == 1;
    }

    @Override
    ValueSetter getValueSetter(String propName) {
        return properties.get(propName);
    }
}
