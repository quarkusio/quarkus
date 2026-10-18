package io.quarkus.it.spring.cache;

public class Greeting {

    private final String message;
    private final Integer count;

    public Greeting(String message, Integer count) {
        this.message = message;
        this.count = count;
    }

    public String getMessage() {
        return message;
    }

    public Integer getCount() {
        return count;
    }
}
