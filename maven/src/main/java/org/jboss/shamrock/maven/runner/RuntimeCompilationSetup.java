package org.jboss.shamrock.maven.runner;

import static org.jboss.shamrock.annotations.ExecutionTime.STATIC_INIT;

import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.annotations.ExecutionTime;
import org.jboss.shamrock.annotations.Record;
import org.jboss.shamrock.runtime.ConfiguredValue;
import org.jboss.shamrock.undertow.runtime.UndertowDeploymentTemplate;

import io.undertow.server.HandlerWrapper;

public class RuntimeCompilationSetup {

    @BuildStep
    @Record(STATIC_INIT)
    public void build(RuntimeCompilationTemplate runtimeCompilationTemplate, UndertowDeploymentTemplate undertowDeploymentTemplate) throws Exception {
        try {
            //don't do this if we don't have Undertow
            Class.forName("org.jboss.shamrock.undertow.ServletResourceProcessor");
        } catch (ClassNotFoundException e) {
            return;
        }
        String classesDir = System.getProperty("shamrock.runner.classes");
        if (classesDir != null) {
            HandlerWrapper wrapper = runtimeCompilationTemplate.createHandlerWrapper();
            undertowDeploymentTemplate.startUndertowEagerly(new ConfiguredValue("http.port", "8080"), new ConfiguredValue("http.host", "localhost"), new ConfiguredValue("http.io-threads", ""), new ConfiguredValue("http.worker-threads", ""), wrapper);
        }
    }


}
