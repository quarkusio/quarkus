package io.quarkus.resteasy.test.subresource;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MyService {

    public String ping() {
        return "pong";
    }

}
