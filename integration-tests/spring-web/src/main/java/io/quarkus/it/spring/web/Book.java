package io.quarkus.it.spring.web;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Book {

    private String name;

    public Book() {
    }

    public Book(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
