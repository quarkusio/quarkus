package io.quarkus.spring.security.deployment.springapp;

public interface PersonChecker {

    boolean check(Person person, String input);

    boolean isTrue();

    boolean isFalse();
}
