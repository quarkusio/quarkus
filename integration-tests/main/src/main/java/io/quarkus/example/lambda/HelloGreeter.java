package io.quarkus.example.lambda;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class HelloGreeter {

    public String greet(String first, String last) {
        return String.format("Hello %s %s.", first, last);
    }

}
