package org.acme.funqy;

import io.quarkus.funqy.Funq;

public class GreetingFunction {

    @Funq
    public String myFunqyGreeting(Person friend) {
        return "Hello " + friend.getName();
    }
}
