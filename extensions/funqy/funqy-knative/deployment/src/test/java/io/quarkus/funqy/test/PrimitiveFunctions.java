package io.quarkus.funqy.test;

import io.quarkus.funqy.Funq;

public class PrimitiveFunctions {
    @Funq
    public String toLowerCase(String val) {
        return val.toLowerCase();
    }

    @Funq
    public int doubleIt(int val) {
        return val * 2;
    }

}
