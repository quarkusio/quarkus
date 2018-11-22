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

package org.jboss.shamrock.maven.runner;

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

public class RuntimeCompilationSetup {

    private static Logger log = Logger.getLogger(RuntimeCompilationSetup.class.getName());

    public static void setup() throws Exception {
        try {
            //don't do this if we don't have Undertow
            Class.forName("org.jboss.shamrock.undertow.runtime.UndertowDeploymentTemplate");
        } catch (ClassNotFoundException e) {
            return;
        }
        String classesDir = System.getProperty("shamrock.runner.classes");
        if (classesDir != null) {
            HandlerWrapper wrapper = createHandlerWrapper();
            //TODO: we need to get these values from the config in runtime mode
            HttpConfig config = new HttpConfig();
            config.port = ShamrockConfig.getInt("shamrock.http.port", "8080");
            config.host = ShamrockConfig.getString("shamrock.http.host", "localhost", true);
            config.ioThreads = Optional.empty();
            config.workerThreads = Optional.empty();

            UndertowDeploymentTemplate.startUndertowEagerly(config, wrapper);
        }
    }


    private static HandlerWrapper createHandlerWrapper() {

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
