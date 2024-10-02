package io.quarkus.security.test.cdi.inheritance;

import jakarta.annotation.security.DenyAll;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@DenyAll
public class DenyAllBean {

    public String ping() {
        return DenyAllBean.class.getSimpleName();
    }
}
