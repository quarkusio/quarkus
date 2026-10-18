package io.quarkus.arc.test.decorators.priority;

interface Converter<T> {

    T convert(T value);

}