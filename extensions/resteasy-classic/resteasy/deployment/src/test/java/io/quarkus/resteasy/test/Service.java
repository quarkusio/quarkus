package io.quarkus.resteasy.test;

import javax.inject.Singleton;

@Singleton
public class Service {

    String execute() {
        return "service";
    }
}
