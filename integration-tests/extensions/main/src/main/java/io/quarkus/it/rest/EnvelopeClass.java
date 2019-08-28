package io.quarkus.it.rest;

public class EnvelopeClass<T> {

    private T content;

    public EnvelopeClass(T content) {
        this.content = content;
    }

    public T getContent() {
        return content;
    }
}
