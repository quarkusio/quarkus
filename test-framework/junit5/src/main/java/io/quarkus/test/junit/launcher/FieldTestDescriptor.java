package io.quarkus.test.junit.launcher;

import java.lang.reflect.Method;

import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;

public class FieldTestDescriptor extends AbstractTestDescriptor {

    private final Method testField;

    public FieldTestDescriptor(Method testField, ClassTestDescriptor parent) {
        super( //
                parent.getUniqueId().append("method", testField.getName()), //
                displayName(testField), //
                FieldSource.from(testField) //
        );
        this.testField = testField;
        setParent(parent);
    }

    public Method getTestField() {
        return testField;
    }

    @Override
    public Type getType() {
        return Type.TEST;
    }

    private static String displayName(Method testField) {
        return "TODO display name" + testField; // TODO
    }

}
