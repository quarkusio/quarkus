package io.quarkus.it.legacy.redirect;

import java.util.List;

public class Greetings {
    private String language;
    private List<Greeting> hellos;

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public List<Greeting> getHellos() {
        return hellos;
    }

    public void setHellos(List<Greeting> hellos) {
        this.hellos = hellos;
    }
}
