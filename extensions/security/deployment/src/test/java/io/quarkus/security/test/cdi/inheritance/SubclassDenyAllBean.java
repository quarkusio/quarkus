package io.quarkus.security.test.cdi.inheritance;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SubclassDenyAllBean extends DenyAllBean {
}
