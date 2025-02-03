package io.quarkus.it.kafka;

import java.util.UUID;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class RequestBean {

    private final String id;
    private String name;

    public RequestBean() {
        this.id = UUID.randomUUID().toString();
    }

    @PostConstruct
    void construct() {
        System.out.println("ReqBean constructed " + this.id);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
