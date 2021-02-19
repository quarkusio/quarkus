package io.quarkus.it.legacy.redirect;

public class Welcome extends Salutation {

    @Override
    public String getType() {
        return "Welcome";
    }

}
