package io.quarkus.arc.test.decorators.other;

public interface Converter<T> {

    T convert(T value);

}
