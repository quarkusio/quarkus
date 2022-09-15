package io.quarkus.security.test.cdi.app.denied.unnanotated;

import jakarta.inject.Singleton;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@Singleton
public class BeanWithNoSecurityAnnotations {
    public String unannotated() {
        return "unannotatedOnBeanWithNoAnno";
    }
}
