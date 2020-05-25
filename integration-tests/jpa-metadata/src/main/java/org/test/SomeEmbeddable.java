package org.test;

import javax.persistence.*;

@Embeddable
public class SomeEmbeddable {
    private String name = "";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
