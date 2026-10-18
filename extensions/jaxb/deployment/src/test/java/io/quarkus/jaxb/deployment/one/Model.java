package io.quarkus.jaxb.deployment.one;

import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Model {
    private String name1;

    public String getName1() {
        return name1;
    }

    public void setName1(String name1) {
        this.name1 = name1;
    }
}
