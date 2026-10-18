package io.quarkus.funqy.test;

import io.quarkus.funqy.Funq;

public class NoArgFun {
    @Funq
    public String noArgFun() {
        return "noArgFun";
    }
}
