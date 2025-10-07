package io.quarkus.arc.test.decorators.interceptor.other;

public interface Converter<T> {

    T convert(T value);

}
