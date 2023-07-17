package org.acme;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Other implements Named {

    @Override
    public String getName() {
        return "other";
    }
}
