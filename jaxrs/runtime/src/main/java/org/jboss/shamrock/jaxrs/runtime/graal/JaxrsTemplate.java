package org.jboss.shamrock.jaxrs.runtime.graal;

import javax.enterprise.inject.se.SeContainer;

import org.jboss.shamrock.runtime.ContextObject;

/**
 * Created by bob on 7/31/18.
 */
public class JaxrsTemplate {

    public void setupIntegration(@ContextObject("weld.container")SeContainer container) {
        ShamrockInjectorFactory.CONTAINER = container;
    }

}
