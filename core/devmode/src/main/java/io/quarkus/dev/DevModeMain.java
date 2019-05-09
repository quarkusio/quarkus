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

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.logging.Handler;

import org.jboss.logging.Logger;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.ApplicationClassPredicateBuildItem;
import io.quarkus.runner.RuntimeRunner;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.Timing;
import io.quarkus.runtime.logging.InitialConfigurator;
import io.smallrye.config.SmallRyeConfigProviderResolver;

/**
 * The main entry point for the dev mojo execution
 */
public class DevModeMain {

    public static final String DEV_MODE_CONTEXT = "META-INF/dev-mode-context.dat";
    private static final Logger log = Logger.getLogger(DevModeMain.class);

    private static volatile ClassLoader currentAppClassLoader;
    private static volatile URLClassLoader runtimeCl;
    private static File classesRoot;
    private static File wiringDir;
    private static File cacheDir;
    private static DevModeContext context;

    private static Closeable runner;
    static volatile Throwable deploymentProblem;
    static RuntimeUpdatesProcessor runtimeUpdatesProcessor;

    public static void main(String... args) throws Exception {
        Timing.staticInitStarted();

        try (InputStream devModeCp = DevModeMain.class.getClassLoader().getResourceAsStream(DEV_MODE_CONTEXT)) {
            context = (DevModeContext) new ObjectInputStream(new DataInputStream(devModeCp)).readObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //propagate system props
        for (Map.Entry<String, String> i : context.getSystemProperties().entrySet()) {
            if (!System.getProperties().containsKey(i.getKey())) {
                System.setProperty(i.getKey(), i.getValue());
            }
        }
        //the path that contains the compiled classes
        classesRoot = new File(args[0]);
        wiringDir = new File(args[1]);
        cacheDir = new File(args[2]);

        runtimeUpdatesProcessor = RuntimeCompilationSetup.setup(context);
        if (runtimeUpdatesProcessor != null) {
            runtimeUpdatesProcessor.checkForChangedClasses();
        }
        //TODO: we can't handle an exception on startup with hot replacement, as Undertow might not have started

        doStart();
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (DevModeMain.class) {
                    if (runner != null) {
                        try {
                            runner.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (runtimeCl != null) {
                        try {
                            runtimeCl.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }, "Quarkus Shutdown Thread"));

        LockSupport.park();
    }

    private static synchronized void doStart() {
        try {
            runtimeCl = new URLClassLoader(new URL[] { classesRoot.toURL() }, ClassLoader.getSystemClassLoader());
            currentAppClassLoader = runtimeCl;
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            //we can potentially throw away this class loader, and reload the app
            try {
                Thread.currentThread().setContextClassLoader(runtimeCl);
                RuntimeRunner.Builder builder = RuntimeRunner.builder()
                        .setLaunchMode(LaunchMode.DEVELOPMENT)
                        .setClassLoader(runtimeCl)
                        .setTarget(classesRoot.toPath())
                        .setFrameworkClassesPath(wiringDir.toPath())
                        .setTransformerCache(cacheDir.toPath());

                List<Path> addAdditionalHotDeploymentPaths = new ArrayList<>();
                for (DevModeContext.ModuleInfo i : context.getModules()) {
                    if (i.getClassesPath() != null) {
                        Path classesPath = Paths.get(i.getClassesPath());
                        addAdditionalHotDeploymentPaths.add(classesPath);
                        builder.addAdditionalHotDeploymentPath(classesPath);
                    }
                }
                // Make it possible to identify wiring classes generated for classes from additional hot deployment paths
                builder.addChainCustomizer(new Consumer<BuildChainBuilder>() {
                    @Override
                    public void accept(BuildChainBuilder buildChainBuilder) {
                        buildChainBuilder.addBuildStep(new BuildStep() {
                            @Override
                            public void execute(BuildContext context) {
                                context.produce(new ApplicationClassPredicateBuildItem(n -> {
                                    return getClassInApplicationClassPaths(n, addAdditionalHotDeploymentPaths) != null;
                                }));
                            }
                        }).produces(ApplicationClassPredicateBuildItem.class).build();
                    }
                });

                RuntimeRunner runner = builder
                        .build();
                runner.run();
                DevModeMain.runner = runner;
                deploymentProblem = null;
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }
        } catch (Throwable t) {
            deploymentProblem = t;
            log.error("Failed to start quarkus", t);

            // if the log handler is not activated, activate it with a default configuration to flush the messages
            if (!InitialConfigurator.DELAYED_HANDLER.isActivated()) {
                InitialConfigurator.DELAYED_HANDLER.setHandlers(new Handler[] { InitialConfigurator.createDefaultHandler() });
            }
        }
    }

    public static synchronized void restartApp() {
        if (runner != null) {
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(runtimeCl);
            try {
                runner.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }
        }
        SmallRyeConfigProviderResolver.instance().releaseConfig(SmallRyeConfigProviderResolver.instance().getConfig());
        DevModeMain.runner = null;
        Timing.restart();
        doStart();
    }

    public static ClassLoader getCurrentAppClassLoader() {
        return currentAppClassLoader;
    }

    private static Path getClassInApplicationClassPaths(String name, List<Path> addAdditionalHotDeploymentPaths) {
        final String fileName = name.replace('.', '/') + ".class";
        Path classLocation;
        for (Path i : addAdditionalHotDeploymentPaths) {
            classLocation = i.resolve(fileName);
            if (Files.exists(classLocation)) {
                return classLocation;
            }
        }
        return null;
    }
}
