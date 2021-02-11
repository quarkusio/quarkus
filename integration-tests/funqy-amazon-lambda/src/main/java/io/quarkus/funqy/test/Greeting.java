package io.quarkus.funqy.test;

import java.util.Objects;

public class Greeting {
    private String name;
    private String message;

    public Greeting() {
    }

    public Greeting(String name, String message) {
        this.name = name;
        this.message = message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Greeting greeting = (Greeting) o;
        return Objects.equals(name, greeting.name) &&
                Objects.equals(message, greeting.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, message);
    }

    @Override
    public String toString() {
        return "Greeting{" +
                "name='" + name + '\'' +
                ", message='" + message + '\'' +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
