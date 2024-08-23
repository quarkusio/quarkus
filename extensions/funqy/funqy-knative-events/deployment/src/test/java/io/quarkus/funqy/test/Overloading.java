package io.quarkus.funqy.test;

import io.quarkus.funqy.Funq;

public class Overloading {

    @Funq("intfun")
    public int function(int i) {
        return i * 2;
    }

    @Funq("strfun")
    public String function(String s) {
        return s.toUpperCase();
    }

}
