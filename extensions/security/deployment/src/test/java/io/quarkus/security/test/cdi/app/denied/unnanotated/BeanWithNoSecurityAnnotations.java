package io.quarkus.security.test.cdi.app.denied.unnanotated;

import jakarta.inject.Singleton;


@Singleton
public class BeanWithNoSecurityAnnotations {
    public String unannotated() {
        return "unannotatedOnBeanWithNoAnno";
    }
}
