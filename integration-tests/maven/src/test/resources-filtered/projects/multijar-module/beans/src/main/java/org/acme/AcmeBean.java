package org.acme;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AcmeBean implements Named {

    @Override
    public String getName() {
        return "acme";
    }
}
