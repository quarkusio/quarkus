package org.acme;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class HelloService {

    public String name() {
        return "Stuart";
    }

}
