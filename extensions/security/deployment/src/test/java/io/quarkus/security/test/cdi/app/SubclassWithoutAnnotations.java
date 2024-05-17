package io.quarkus.security.test.cdi.app;

import jakarta.enterprise.context.ApplicationScoped;


@ApplicationScoped
public class SubclassWithoutAnnotations extends SubclassWithDenyAll {
}
