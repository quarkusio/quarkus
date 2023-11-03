package io.quarkus.it.jackson.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class CustomTypeResolver extends TypeIdResolverBase {

    private JavaType baseType;

    public CustomTypeResolver() {
    }

    @Override
    public void init(JavaType bt) {
        baseType = bt;
    }

    @Override
    public String idFromValue(Object value) {
        return getId(value);
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> suggestedType) {
        return getId(value);
    }

    @Override
    public JsonTypeInfo.Id getMechanism() {
        return JsonTypeInfo.Id.CUSTOM;
    }

    private String getId(Object value) {
        if (value instanceof ModelWithJsonTypeIdResolver) {
            return ((ModelWithJsonTypeIdResolver) value).getType();
        }

        return null;
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) {
        if (id != null) {
            switch (id) {
                case "ONE":
                    return context.constructSpecializedType(baseType, ModelWithJsonTypeIdResolver.SubclassOne.class);
                case "TWO":
                    return context.constructSpecializedType(baseType, ModelWithJsonTypeIdResolver.SubclassTwo.class);
            }
        }
        return TypeFactory.unknownType();
    }
}
