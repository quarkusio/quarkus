package io.quarkus.resteasy.reactive.jaxb.deployment.test.one;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * This class would make the JAXBContext to fail because there is an existing class with same name and model in the
 * package `one`.
 * To address this failure, we are excluding the model classes using the property `quarkus.jaxb.exclude-classes` in the
 * `application.properties` file.
 */
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
