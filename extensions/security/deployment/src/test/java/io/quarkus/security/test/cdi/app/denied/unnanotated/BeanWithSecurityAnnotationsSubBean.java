package io.quarkus.security.test.cdi.app.denied.unnanotated;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@ApplicationScoped
public class BeanWithSecurityAnnotationsSubBean extends BeanWithSecurityAnnotations {

}
