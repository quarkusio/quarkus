package org.acme;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ExternalService {

    public String service() {
        return "external";
    }

}
