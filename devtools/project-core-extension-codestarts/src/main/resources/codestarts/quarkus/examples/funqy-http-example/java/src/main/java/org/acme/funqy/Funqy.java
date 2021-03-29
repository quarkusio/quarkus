package org.acme.funqy;

import io.quarkus.funqy.Funq;

import java.util.Random;

public class Funqy {

    private static final String CHARM_QUARK_SYMBOL = "c";

    @Funq
    public String charm(Answer answer) {
        return CHARM_QUARK_SYMBOL.equalsIgnoreCase(answer.value) ? "You Quark!" : "👻 Wrong answer";
    }

    public static class Answer {
        public String value;
    }
}
