package org.jboss.shamrock.maven.runner;

import java.io.File;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.shamrock.runtime.Template;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;

@Template
public class RuntimeCompilationTemplate {

    private static final Logger log = Logger.getLogger(RuntimeCompilationTemplate.class.getName());

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
