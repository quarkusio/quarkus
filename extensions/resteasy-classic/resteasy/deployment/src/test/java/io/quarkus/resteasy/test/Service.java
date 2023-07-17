package io.quarkus.resteasy.test;

import jakarta.inject.Singleton;

@Singleton
public class Service {

    String execute() {
        return "service";
    }
}
