package io.quarkus.arc.app.classes.tests;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SomeService {

    public String execute() {
        return "some";
    }
}
