package io.quarkus.security.test.cdi.app.denied.unnanotated;

import javax.annotation.security.PermitAll;
import javax.enterprise.context.ApplicationScoped;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@PermitAll
@ApplicationScoped
public class PermitAllBean {

    public String unannotated() {
        return "unannotated";
    }
}
