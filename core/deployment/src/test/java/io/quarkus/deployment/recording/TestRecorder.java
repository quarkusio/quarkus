package io.quarkus.deployment.recording;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.RelaxedValidation;

public class TestRecorder {

    public static final ArrayDeque<Object> RESULT = new ArrayDeque<Object>();

    public void primitiveParams(boolean bool, byte b, char c, short s, int i, long l, float f, double d) {
        RESULT.add(bool);
        RESULT.add(b);
        RESULT.add(c);
        RESULT.add(s);
        RESULT.add(i);
        RESULT.add(l);
        RESULT.add(f);
        RESULT.add(d);
    }

    public void boxedPrimitiveParams(Boolean bool, Byte b, Character c, Short s, Integer i, Long l, Float f, Double d) {
        RESULT.add(bool);
        RESULT.add(b);
        RESULT.add(c);
        RESULT.add(s);
        RESULT.add(i);
        RESULT.add(l);
        RESULT.add(f);
        RESULT.add(d);
    }

    public void intArray(int... args) {
        RESULT.add(args);
    }

    public void doubleArray(double... args) {
        RESULT.add(args);
    }

    public void list(List<?> args) {
        RESULT.add(args);
    }

    public void set(Set<?> args) {
        RESULT.add(args);
    }

    public void map(Map<?, ?> args) {
        RESULT.add(args);
    }

    public void bean(TestJavaBean bean) {
        RESULT.add(bean);
    }

    public void bean(TestJavaBeanWithBoolean bean) {
        RESULT.add(bean);
    }

    public void bean(NonSerializable bean) {
        RESULT.add(bean);
    }

    public void add(RuntimeValue<TestJavaBean> bean) {
        bean.getValue().setIval(bean.getValue().getIval() + 1);
        bean.getValue().setBoxedIval(bean.getValue().getBoxedIval() + 1);
    }

    public void bean(TestConstructorBean bean) {
        RESULT.add(bean);
    }

    public void result(RuntimeValue<TestJavaBean> bean) {
        RESULT.add(bean.getValue());
    }

    public void array(Object[] toArray) {
        RESULT.add(toArray);
    }

    public void object(Object obj) {
        RESULT.add(obj);
    }

    public Supplier<String> stringSupplier(String val) {
        return new Supplier<String>() {
            @Override
            public String get() {
                return val;
            }
        };
    }

    public void relaxedObject(@RelaxedValidation ValidationFails validationFails) {
        RESULT.add(validationFails);
    }

    public void ignoredProperties(IgnoredProperties ignoredProperties) {
        RESULT.add(ignoredProperties);
    }
}
