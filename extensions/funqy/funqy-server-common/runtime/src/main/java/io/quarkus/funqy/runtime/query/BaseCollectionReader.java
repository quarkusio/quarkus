package io.quarkus.funqy.runtime.query;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.quarkus.arc.impl.Reflections;

public abstract class BaseCollectionReader extends BaseObjectReader implements BaseObjectReader.ValueSetter {
    protected Function<String, Object> valueExtractor;
    protected QueryPropertySetter setter;

    public BaseCollectionReader(Type genericType, QueryObjectMapper mapper) {
        if (genericType == null) {
            valueExtractor = mapper.extractor(String.class);
            return;
        }
        if (genericType instanceof ParameterizedType) {
            Type valueType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
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
            Class<Object> rawType = Reflections.getRawType(genericType);
            valueExtractor = mapper.extractor(rawType);
            if (valueExtractor == null) {
                setter = mapper.setterFor(rawType, genericType);
            }
        }
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
    public void setValue(Object target, String propName, Object value) {
        ((Collection) target).add(value);
    }

    @Override
    public void setValue(Object target, String name, String value, Map<String, List<Object>> paramToObject) {
        if (valueExtractor != null) {
            if (name != null)
                return; // ignore query parameter
            ((Collection) target).add(valueExtractor.apply(value));
        } else {
            super.setValue(target, name, value, paramToObject);
        }
    }
}
