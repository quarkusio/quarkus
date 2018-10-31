package org.jboss.shamrock.camel.runtime;

import java.util.logging.Logger;

import org.jboss.shamrock.runtime.InjectionInstance;

public class CamelDeploymentTemplate {

    private static final Logger log = Logger.getLogger(CamelDeploymentTemplate.class.getName());

    static CamelRuntime runtime;

    public void init() {
        System.getProperty("CamelSimpleLRUCacheFactory", "true");
    }

    public void run(InjectionInstance<? extends CamelRuntime> ii) throws Exception {
        System.err.println("\nRunning Camel\n");
        runtime = ii.newInstance();
        runtime.run(new String[0]);
    }

}
