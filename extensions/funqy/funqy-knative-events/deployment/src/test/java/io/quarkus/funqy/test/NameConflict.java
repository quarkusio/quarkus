package io.quarkus.funqy.test;

import io.quarkus.funqy.Funq;

public class NameConflict {

    @Funq
    public String function(String s) {
        return s.toUpperCase();
    }

    @Funq
    public int function(int i) {
        return i * 2;
    }

}
