package io.quarkus.spring.security.deployment.springapp;

public class PersonCheckerImpl implements PersonChecker {

    @Override
    public boolean check(Person person, String input) {
        return person.getName().equals(input);
    }

    @Override
    public boolean isTrue() {
        return true;
    }

    @Override
    public boolean isFalse() {
        return false;
    }
}
