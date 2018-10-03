package org.jboss.shamrock.maven.runner;

import java.io.Closeable;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

import org.jboss.shamrock.deployment.ArchiveContextBuilder;
import org.jboss.shamrock.runtime.Timing;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;

/**
 * The main entry point for the run mojo execution
 */
public class RunMojoMain {

    private static CountDownLatch awaitChangeLatch = null;
    private static CountDownLatch awaitRestartLatch = null;
    private static volatile boolean keepCl = false;
    private static volatile ClassLoader currentAppClassLoader;

    public static void main(String... args) throws Exception {
        Timing.staticInitStarted();
        //the path that contains the compiled classes
        File classesRoot = new File(args[0]);
        File wiringDir = new File(args[1]);
        URLClassLoader runtimeCl = null;
        do {
            if (runtimeCl == null || !keepCl) {
                runtimeCl = new URLClassLoader(new URL[]{classesRoot.toURL()}, ClassLoader.getSystemClassLoader());
            }
            URL resource = runtimeCl.getResource("logback.xml"); //TODO: this is temporary, until David's logging thing comes it, but not having it is super annoying for demos
            if(resource != null) {
                LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
                loggerContext.reset();
                JoranConfigurator configurator = new JoranConfigurator();
                try (InputStream configStream = resource.openStream()) {
                    configurator.setContext(loggerContext);
                    configurator.doConfigure(configStream); // loads logback file
                }
            }

            currentAppClassLoader = runtimeCl;
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            //we can potentially throw away this class loader, and reload the app
            synchronized (RunMojoMain.class) {
                awaitChangeLatch = new CountDownLatch(1);
            }
            try {
                Thread.currentThread().setContextClassLoader(runtimeCl);
                Class<?> runnerClass = runtimeCl.loadClass("org.jboss.shamrock.runner.RuntimeRunner");
                ArchiveContextBuilder acb = new ArchiveContextBuilder();
                Constructor ctor = runnerClass.getDeclaredConstructor( ClassLoader.class, Path.class, Path.class, ArchiveContextBuilder.class);
                Object runner = ctor.newInstance( runtimeCl, classesRoot.toPath(), wiringDir.toPath(), acb);
                ((Runnable) runner).run();
                synchronized (RunMojoMain.class) {
                    if (awaitRestartLatch != null) {
                        awaitRestartLatch.countDown();
                        awaitRestartLatch = null;
                    }
                }
                awaitChangeLatch.await();
                ((Closeable) runner).close();
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }

        } while (true);
    }

    public static void restartApp(boolean keepClassloader) {
        keepCl = keepClassloader;
        Timing.restart();
        CountDownLatch restart = null;
        synchronized (RunMojoMain.class) {
            if (awaitChangeLatch != null) {
                restart = awaitRestartLatch = new CountDownLatch(1);
                awaitChangeLatch.countDown();
            }
            awaitChangeLatch = null;
        }
        if (restart != null) {
            try {
                restart.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static ClassLoader getCurrentAppClassLoader() {
        return currentAppClassLoader;
    }
}
