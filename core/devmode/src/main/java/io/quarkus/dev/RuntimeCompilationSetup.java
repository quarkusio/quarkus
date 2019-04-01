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

package io.quarkus.dev;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.quarkus.deployment.devmode.HotReplacementSetup;

public class RuntimeCompilationSetup {

    public static final String PROP_RUNNER_CLASSES = "quarkus-internal.runner.classes";
    public static final String PROP_RUNNER_SOURCES = "quarkus-internal.runner.sources";
    public static final String PROP_RUNNER_RESOURCES = "quarkus-internal.runner.resources";

    private static Logger log = Logger.getLogger(RuntimeCompilationSetup.class.getName());

    public static RuntimeUpdatesProcessor setup() throws Exception {
        String classesDir = System.getProperty(PROP_RUNNER_CLASSES);
        String sourcesDir = System.getProperty(PROP_RUNNER_SOURCES);
        String resourcesDir = System.getProperty(PROP_RUNNER_RESOURCES);
        if (classesDir != null) {
            ServiceLoader<CompilationProvider> serviceLoader = ServiceLoader.load(CompilationProvider.class);
            List<CompilationProvider> compilationProviders = new ArrayList<>();
            serviceLoader.iterator().forEachRemaining(compilationProviders::add);
            ClassLoaderCompiler compiler = null;
            try {
                compiler = new ClassLoaderCompiler(Thread.currentThread().getContextClassLoader(), new File(classesDir),
                        compilationProviders);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Failed to create compiler, runtime compilation will be unavailable", e);
                return null;
            }
            RuntimeUpdatesProcessor processor = new RuntimeUpdatesProcessor(Paths.get(classesDir),
                    sourcesDir == null ? null : Paths.get(sourcesDir), resourcesDir == null ? null : Paths.get(resourcesDir),
                    compiler);

            for (HotReplacementSetup service : ServiceLoader.load(HotReplacementSetup.class)) {
                service.setupHotDeployment(processor);
            }
            return processor;
        }
        return null;
    }
}
