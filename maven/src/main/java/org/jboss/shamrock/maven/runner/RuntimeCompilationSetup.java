package org.jboss.shamrock.maven.runner;

import java.io.File;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.shamrock.deployment.ArchiveContext;
import org.jboss.shamrock.deployment.ProcessorContext;
import org.jboss.shamrock.deployment.ResourceProcessor;
import org.jboss.shamrock.deployment.RuntimePriority;
import org.jboss.shamrock.deployment.SetupContext;
import org.jboss.shamrock.deployment.ShamrockSetup;
import org.jboss.shamrock.deployment.codegen.BytecodeRecorder;
import org.jboss.shamrock.runtime.ConfiguredValue;
import org.jboss.shamrock.undertow.runtime.UndertowDeploymentTemplate;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;

public class RuntimeCompilationSetup implements ShamrockSetup {

    private static final Logger log = Logger.getLogger(RuntimeCompilationTemplate.class.getName());

    @Override
    public void setup(SetupContext context) {
        try {
            //don't do this if we don't have Undertow
            Class.forName("org.jboss.shamrock.undertow.ServletResourceProcessor");
        } catch (ClassNotFoundException e) {
            return;
        }

        String classesDir = System.getProperty("shamrock.runner.classes");
        if (classesDir != null) {
            context.addResourceProcessor(new ResourceProcessor() {
                @Override
                public void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception {
                    try (BytecodeRecorder recorder = processorContext.addStaticInitTask(RuntimePriority.HOT_DEPLOYMENT_START_UNDERTOW)) {
                        HandlerWrapper wrapper = recorder.getRecordingProxy(RuntimeCompilationTemplate.class).createHandlerWrapper();
                        recorder.getRecordingProxy(UndertowDeploymentTemplate.class).startUndertowEagerly(new ConfiguredValue("http.port", "8080"), new ConfiguredValue("http.host", "localhost"), new ConfiguredValue("http.io-threads",""), new ConfiguredValue("http.worker-threads",""), wrapper);
                    }
                }

                @Override
                public int getPriority() {
                    return RuntimePriority.HOT_DEPLOYMENT_HANDLER;
                }
            });
        }


    }


    public static class RuntimeCompilationTemplate {

        public HandlerWrapper createHandlerWrapper() {

            String classesDir = System.getProperty("shamrock.runner.classes");
            String sourcesDir = System.getProperty("shamrock.runner.sources");

            return new HandlerWrapper() {
                @Override
                public HttpHandler wrap(HttpHandler handler) {
                    ClassLoaderCompiler compiler = null;
                    try {
                        compiler = new ClassLoaderCompiler(Thread.currentThread().getContextClassLoader(), new File(classesDir));
                    } catch (Exception e) {
                        log.log(Level.SEVERE, "Failed to create compiler, runtime compilation will be unavailable", e);
                        return handler;
                    }
                    return new RuntimeUpdatesHandler(handler, Paths.get(classesDir), Paths.get(sourcesDir), compiler);
                }
            };
        }

    }
}
