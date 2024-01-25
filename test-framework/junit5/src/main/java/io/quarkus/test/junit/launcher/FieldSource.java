package io.quarkus.test.junit.launcher;

import java.lang.reflect.Method;

import org.junit.platform.engine.TestSource;

public class FieldSource implements TestSource {

    private static final long serialVersionUID = 1L;

    private final String className;
    private final String fieldName;

    public static FieldSource from(Method testField) {
        return new FieldSource(testField.getDeclaringClass().toGenericString(), testField.getName());
    }

    public FieldSource(String className, String fieldName) {
        this.className = className;
        this.fieldName = fieldName;
    }

}
