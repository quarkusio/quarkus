package io.quarkus.it.smallrye.graphql;

public class Farewell extends Salutation {

    @Override
    public String getType() {
        return "Farewell";
    }

}
