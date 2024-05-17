package io.quarkus.security.test.cdi.app.denied.unnanotated;

import jakarta.enterprise.context.ApplicationScoped;


@ApplicationScoped
public class BeanWithSecurityAnnotationsSubBean extends BeanWithSecurityAnnotations {

}
