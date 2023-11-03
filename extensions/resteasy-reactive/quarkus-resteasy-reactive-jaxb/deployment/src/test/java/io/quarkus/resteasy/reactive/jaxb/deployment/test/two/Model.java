package io.quarkus.resteasy.reactive.jaxb.deployment.test.two;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * This class would make the JAXBContext to fail because there is an existing class with same name and model in the
 * package `one`.
 * To address this failure, we are excluding the model classes using the property `quarkus.jaxb.exclude-classes` in the
 * `application.properties` file.
 */
@XmlRootElement
public class Model {
    private String name2;

    public String getName2() {
        return name2;
    }

    public void setName2(String name2) {
        this.name2 = name2;
    }
}
