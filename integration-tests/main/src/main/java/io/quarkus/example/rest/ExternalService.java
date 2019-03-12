package io.quarkus.example.rest;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ExternalService {

    public String service() {
        return "external";
    }

}
