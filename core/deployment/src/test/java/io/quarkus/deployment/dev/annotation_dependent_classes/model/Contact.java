package io.quarkus.deployment.dev.annotation_dependent_classes.model;

public class Contact extends ModelBase {
    private String name;

    private Email email;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Email getEmail() {
        return email;
    }

    public void setEmail(Email email) {
        this.email = email;
    }
}
