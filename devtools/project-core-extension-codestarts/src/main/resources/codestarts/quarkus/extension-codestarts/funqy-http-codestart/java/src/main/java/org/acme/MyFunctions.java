package org.acme;

import io.quarkus.funqy.Funq;

public class MyFunctions {

    @Funq
    public String fun(FunInput input) {
        return String.format("Hello %s!", input != null ? input.name : "Funqy");
    }


    public static class FunInput {
        public String name;
    }

}
