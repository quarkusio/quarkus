package io.quarkus.it.smallrye.graphql;

public class Welcome extends Salutation {

    @Override
    public String getType() {
        return "Welcome";
    }

}
