package io.quarkus.funqy.test;

import io.quarkus.funqy.Funq;

public class SingleFunction {

    @Funq
    public String echo(String msg) {
        return msg;
    }
}
