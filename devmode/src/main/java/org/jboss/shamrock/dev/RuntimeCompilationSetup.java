/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.shamrock.dev;

import java.io.File;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.shamrock.deployment.ShamrockConfig;
import org.jboss.shamrock.undertow.runtime.HttpConfig;
import org.jboss.shamrock.undertow.runtime.UndertowDeploymentTemplate;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

public class RuntimeCompilationSetup {

    private static Logger log = Logger.getLogger(RuntimeCompilationSetup.class.getName());

    public static RuntimeUpdatesProcessor setup() throws Exception {
        try {
            //don't do this if we don't have Undertow
            Class.forName("org.jboss.shamrock.undertow.runtime.UndertowDeploymentTemplate");
        } catch (ClassNotFoundException e) {
            return null;
        }
        String classesDir = System.getProperty("shamrock.runner.classes");
        String sourcesDir = System.getProperty("shamrock.runner.sources");
        String resourcesDir = System.getProperty("shamrock.runner.resources");
        if (classesDir != null) {
            ClassLoaderCompiler compiler = null;
            try {
                compiler = new ClassLoaderCompiler(Thread.currentThread().getContextClassLoader(), new File(classesDir));
            } catch (Exception e) {
                log.log(Level.SEVERE, "Failed to create compiler, runtime compilation will be unavailable", e);
                return null;
            }
            RuntimeUpdatesProcessor processor = new RuntimeUpdatesProcessor(Paths.get(classesDir), Paths.get(sourcesDir), Paths.get(resourcesDir), compiler);
            HandlerWrapper wrapper = createHandlerWrapper(processor);
            //TODO: we need to get these values from the config in runtime mode
            HttpConfig config = new HttpConfig();
            config.port = ShamrockConfig.getInt("shamrock.http.port", "8080");
            config.host = ShamrockConfig.getString("shamrock.http.host", "localhost", true);
            config.ioThreads = Optional.empty();
            config.workerThreads = Optional.empty();

            UndertowDeploymentTemplate.startUndertowEagerly(config, wrapper);
            return processor;
        }
        return null;
    }


    private static HandlerWrapper createHandlerWrapper(RuntimeUpdatesProcessor processor) {


        return new HandlerWrapper() {
            @Override
            public HttpHandler wrap(HttpHandler handler) {
                return new HttpHandler() {
                    @Override
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        if (exchange.isInIoThread()) {
                            exchange.dispatch(this);
                            return;
                        }
                        processor.handleRequest(exchange, handler);
                    }
                };
            }
        };
    }
}
