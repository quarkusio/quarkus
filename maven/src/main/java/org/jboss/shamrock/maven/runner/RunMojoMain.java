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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;
import org.jboss.shamrock.runtime.Timing;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfigProviderResolver;

/**
 * The main entry point for the run mojo execution
 */
public class RunMojoMain {

    private static final Logger log = Logger.getLogger(RunMojoMain.class);

    private static volatile boolean keepCl = false;
    private static volatile ClassLoader currentAppClassLoader;
    private static volatile URLClassLoader runtimeCl;
    private static File classesRoot;
    private static File wiringDir;
    private static File cacheDir;

    private static Closeable closeable;
    static volatile Throwable deploymentProblem;

    public static void main(String... args) throws Exception {

        Timing.staticInitStarted();



        //the path that contains the compiled classes
        classesRoot = new File(args[0]);
        wiringDir = new File(args[1]);
        cacheDir = new File(args[2]);

        //first lets look for some config, as it is not on the current class path
        //and we need to load it to start undertow eagerly
        File config = new File(classesRoot, "META-INF/microprofile-config.properties");
        if(config.exists()) {
            try {
                Config built = SmallRyeConfigProviderResolver.instance().getBuilder()
                        .addDefaultSources()
                        .addDiscoveredConverters()
                        .addDiscoveredSources()
                        .withSources(new PropertiesConfigSource(config.toURL())).build();
                SmallRyeConfigProviderResolver.instance().registerConfig(built, Thread.currentThread().getContextClassLoader());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        RuntimeCompilationSetup.setup();
        //TODO: we can't handle an exception on startup with hot replacement, as Undertow might not have started

        doStart();
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (RunMojoMain.class) {
                    if (closeable != null) {
                        try {
                            closeable.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }, "Shamrock Shutdown Thread"));
    }

    private static synchronized void doStart() {
        try {
            if (runtimeCl == null || !keepCl) {
                runtimeCl = new URLClassLoader(new URL[]{classesRoot.toURL()}, ClassLoader.getSystemClassLoader());
            }
            currentAppClassLoader = runtimeCl;
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            //we can potentially throw away this class loader, and reload the app
            try {
                Thread.currentThread().setContextClassLoader(runtimeCl);
                Class<?> runnerClass = runtimeCl.loadClass("org.jboss.shamrock.runner.RuntimeRunner");
                Constructor ctor = runnerClass.getDeclaredConstructor(ClassLoader.class, Path.class, Path.class, Path.class, List.class);
                Object runner = ctor.newInstance(runtimeCl, classesRoot.toPath(), wiringDir.toPath(), cacheDir.toPath(), new ArrayList<>());
                ((Runnable) runner).run();
                closeable = ((Closeable) runner);
                deploymentProblem = null;
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }
        } catch (Throwable t) {
            deploymentProblem = t;
            log.error("Failed to start shamrock", t);
        }
    }

    public static synchronized void restartApp(boolean keepClassloader) {
        keepCl = keepClassloader;
        if (closeable != null) {

            ClassLoader old = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(runtimeCl);
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }
        }
        closeable = null;
        doStart();
    }

    public static ClassLoader getCurrentAppClassLoader() {
        return currentAppClassLoader;
    }
}
