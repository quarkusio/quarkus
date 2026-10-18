package io.quarkus.it.openapi;

public class Greeting {
    private int id;
    private String salutation;

    public Greeting() {

    }

    public Greeting(int id, String salutation) {
        this.id = id;
        this.salutation = salutation;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSalutation() {
        return salutation;
    }

    public void setSalutation(String salutation) {
        this.salutation = salutation;
    }
}
