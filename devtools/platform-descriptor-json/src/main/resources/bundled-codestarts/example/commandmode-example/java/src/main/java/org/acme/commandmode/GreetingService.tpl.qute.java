package org.acme.commandmode;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GreetingService {

    public String greeting(String name) {
        return "{greeting.message} " + name;
    }

}