package io.quarkus.deployment.recording;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

final class FieldsHelper {

    private final Map<String, Field> fields;

    public FieldsHelper(final Class<?> aClass) {
        final Field[] declaredFields = aClass.getDeclaredFields();
        this.fields = new HashMap<>(declaredFields.length);
        for (Field field : declaredFields) {
            this.fields.put(field.getName(), field);
        }
    }

    //Returns the matching Field, or null if not existing
    public Field getDeclaredField(final String name) {
        return fields.get(name);
    }
}
