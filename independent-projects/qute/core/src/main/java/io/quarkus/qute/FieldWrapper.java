package io.quarkus.qute;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

/**
 *
 * @author Martin Kouba
 */
class FieldWrapper implements MemberWrapper {

    private final Field field;

    FieldWrapper(Field field) {
        super();
        this.field = field;
    }

    @Override
    public Object getValue(Object instance) throws IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        return field.get(instance);
    }

}
