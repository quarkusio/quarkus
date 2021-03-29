package io.quarkus.qute.deployment;

public class HelloReflect {

    public Long age = 10l;

    public String ping() {
        return "pong";
    }

    public boolean isActive() {
        return true;
    }

    public Long getAge2() {
        return age;
    }

    public boolean hasItem() {
        return false;
    }

}