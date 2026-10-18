package org.acme.nested;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SimpleService {
    public String hello(){
        return "included-kordamp";
    }
}
