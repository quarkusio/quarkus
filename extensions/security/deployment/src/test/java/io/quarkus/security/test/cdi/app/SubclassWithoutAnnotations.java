package io.quarkus.security.test.cdi.app;

import javax.enterprise.context.ApplicationScoped;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@ApplicationScoped
public class SubclassWithoutAnnotations extends SubclassWithDenyAll {
}
