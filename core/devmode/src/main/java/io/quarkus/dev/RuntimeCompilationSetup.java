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

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.quarkus.deployment.devmode.HotReplacementSetup;

public class RuntimeCompilationSetup {

    private static Logger log = Logger.getLogger(RuntimeCompilationSetup.class.getName());

    public static RuntimeUpdatesProcessor setup(DevModeContext context) throws Exception {
        if (!context.getModules().isEmpty()) {
            ServiceLoader<CompilationProvider> serviceLoader = ServiceLoader.load(CompilationProvider.class);
            List<CompilationProvider> compilationProviders = new ArrayList<>();
            for (CompilationProvider provider : serviceLoader) {
                compilationProviders.add(provider);
                context.getModules().forEach(moduleInfo -> moduleInfo.addSourcePaths(provider.handledSourcePaths()));
            }
            ClassLoaderCompiler compiler;
            try {
                compiler = new ClassLoaderCompiler(Thread.currentThread().getContextClassLoader(),
                        compilationProviders, context);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Failed to create compiler, runtime compilation will be unavailable", e);
                return null;
            }
            RuntimeUpdatesProcessor processor = new RuntimeUpdatesProcessor(context, compiler);

            for (HotReplacementSetup service : ServiceLoader.load(HotReplacementSetup.class)) {
                service.setupHotDeployment(processor);
                processor.addHotReplacementSetup(service);
            }
            return processor;
        }
        return null;
    }
}
