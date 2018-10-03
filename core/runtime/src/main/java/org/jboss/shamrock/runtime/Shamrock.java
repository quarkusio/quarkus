package org.jboss.shamrock.runtime;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The main entry point class, calling main allows you to bootstrap shamrock
 *
 * Note that at native image generation time this is replaced by {@link org.jboss.shamrock.runtime.graal.ShamrockReplacement}
 * which will avoid the need for reflection.
 *
 * TODO: how do we deal with static init
 *
 */
public class Shamrock {

    public static void main(String... args) throws Exception {
        try {
            //check if the main class is on the classpath
            //if so then we have a wiring jar on the classpath, which means
            //we should not be using runtime mode
            Class main = Class.forName("org.jboss.shamrock.runner.GeneratedMain");
            Method mainMethod = main.getDeclaredMethod("main", String[].class);
            mainMethod.invoke(null, (Object) args);
        } catch (Exception e) {
            Logger.getLogger("shamrock").log(Level.WARNING, "Could not find wiring classes, using development mode");
            Runnable runnable;
            try {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                if (cl == null) {
                    cl = Shamrock.class.getClassLoader();
                }
                //TODO: massive hack
                //but I don't know a better way to do this
                //basically we want to find the classes directory, so we can process it
                Path path = null;
                Enumeration<URL> resources = cl.getResources("");
                while (resources.hasMoreElements()) {
                    URL url = resources.nextElement();
                    path = Paths.get(url.toURI());
                    break;
                }
                Class<?> runtimeRunner = Class.forName("org.jboss.shamrock.runtime.RuntimeRunner");
                Constructor<?> ctor = runtimeRunner.getConstructor(Path.class);
                runnable = (Runnable) ctor.newInstance(path);

            } catch (Throwable t) {

                throw new RuntimeException("Could not create Runtime runner. Are the deployment artifacts on the classpath?", t);
            }
            runnable.run();
        }
    }
}
