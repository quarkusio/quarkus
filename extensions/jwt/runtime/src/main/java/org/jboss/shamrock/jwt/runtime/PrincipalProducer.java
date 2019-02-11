package org.jboss.shamrock.jwt.runtime;

import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;

import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Override the default CDI Principal bean to allow the injection of a Principal to be both a
 * {@linkplain JsonWebToken} and a {@linkplain java.security.Principal}.
 */
@Priority(1)
@Alternative
public class PrincipalProducer {

    /**
     * The producer method for the current JsonWebToken
     *
     * @return
     */
    @Produces
    @RequestScoped
    JsonWebToken currentJWTPrincipalOrNull() {
        return MPJWTProducer.getJWTPrincpal();
    }
}
