package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import java.beans.Transient;

public class JavaBeansTransientBean {

    private String name;
    private String visible;
    private String secret;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVisible() {
        return visible;
    }

    public void setVisible(String visible) {
        this.visible = visible;
    }

    @Transient
    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
