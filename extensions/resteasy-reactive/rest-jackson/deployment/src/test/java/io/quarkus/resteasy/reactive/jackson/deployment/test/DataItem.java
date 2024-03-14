package io.quarkus.resteasy.reactive.jackson.deployment.test;

public class DataItem<T> {

    private T content;

    public T getContent() {
        return content;
    }

    public void setContent(T content) {
        this.content = content;
    }
}
