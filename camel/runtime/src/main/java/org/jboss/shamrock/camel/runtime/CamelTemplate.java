package org.jboss.shamrock.camel.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.bind.JAXBContext;

import org.apache.camel.impl.DefaultModelJAXBContextFactory;
import org.jboss.shamrock.runtime.ContextObject;
import org.jboss.shamrock.runtime.InjectionInstance;

public class CamelTemplate {

    @ContextObject("camel.runtimes")
    public List<CamelRuntime> createRuntimes() {
        return new ArrayList<>();
    }

    public void init(@ContextObject("camel.runtimes") List<CamelRuntime> runtimes,
                     InjectionInstance<? extends CamelRuntime> ii,
                     SimpleLazyRegistry registry,
                     Properties properties) throws Exception {
        CamelRuntime runtime = ii.newInstance();
        runtime.setRegistry(registry);
        runtime.setProperties(properties);
        runtime.init();
        runtimes.add(runtime);
    }

    public void run(@ContextObject("camel.runtimes") List<CamelRuntime> runtimes) throws Exception {
        for (CamelRuntime runtime : runtimes) {
            runtime.run();
        }
    }

}
