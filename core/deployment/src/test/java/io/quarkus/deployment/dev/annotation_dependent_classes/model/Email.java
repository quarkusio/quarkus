package io.quarkus.deployment.dev.annotation_dependent_classes.model;

// embeddable, with validation etc
public class Email {
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
