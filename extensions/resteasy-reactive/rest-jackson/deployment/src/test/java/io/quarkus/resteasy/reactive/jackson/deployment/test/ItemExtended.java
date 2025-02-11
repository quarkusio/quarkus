package io.quarkus.resteasy.reactive.jackson.deployment.test;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ItemExtended extends Item {

    private String nameExtended;

    @JsonIgnore
    private String emailExtended;

    public String getNameExtended() {
        return nameExtended;
    }

    public void setNameExtended(String nameExtended) {
        this.nameExtended = nameExtended;
    }

    public String getEmailExtended() {
        return emailExtended;
    }

    public void setEmailExtended(String emailExtended) {
        this.emailExtended = emailExtended;
    }
}
