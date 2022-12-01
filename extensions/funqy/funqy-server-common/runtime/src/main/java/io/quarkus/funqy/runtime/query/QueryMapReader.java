package io.quarkus.funqy.runtime.query;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import io.quarkus.arc.impl.Reflections;

/**
 * Key can be any primitive, primitive object (i.e. Integer), or String
 * Value can be any primitive, primitive object, string, or bean style class
 *
 */
class QueryMapReader extends BaseObjectReader implements BaseObjectReader.ValueSetter {
    private Function<String, Object> keyExtractor;
    private Function<String, Object> valueExtractor;
    private QueryPropertySetter setter;

    public QueryMapReader(Type genericType, QueryObjectMapper mapper) {
        if (genericType == null) {
            keyExtractor = mapper.extractor(String.class);
            valueExtractor = mapper.extractor(String.class);
            return;
        }
        if (genericType instanceof ParameterizedType) {
            Type keyType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
            keyExtractor = mapper.extractor(keyType);
            if (keyType == null)
                throw new RuntimeException("Illegal key type for map");
            Type valueType = ((ParameterizedType) genericType).getActualTypeArguments()[1];
            if (valueType != null) {
                Class<Object> rawType = Reflections.getRawType(valueType);
                valueExtractor = mapper.extractor(valueType);
                if (valueExtractor == null) {
                    setter = mapper.setterFor(rawType, valueType);
                }
            } else {
                valueExtractor = mapper.extractor(String.class);
            }
        } else {
            keyExtractor = mapper.extractor(String.class);
            Class<Object> rawType = Reflections.getRawType(genericType);
            valueExtractor = mapper.extractor(rawType);
            if (valueExtractor == null) {
                setter = mapper.setterFor(rawType, genericType);
            }
        }
    }

    @Override
    public void setValue(Object target, String propName, Object value) {
        ((Map) target).put(keyExtractor.apply(propName), value);
    }

    @Override
    public Function<String, Object> getExtractor() {
        return valueExtractor;
    }

    @Override
    public QueryPropertySetter getSetter() {
        return setter;
    }

    @Override
    ValueSetter getValueSetter(String propName) {
        return this;
    }

    @Override
    public Object create() {
        return new HashMap();
    }

}
