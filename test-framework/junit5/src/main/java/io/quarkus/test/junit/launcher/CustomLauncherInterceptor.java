package io.quarkus.test.junit.launcher;

import org.junit.platform.launcher.LauncherInterceptor;

import io.quarkus.test.junit.classloading.FacadeClassLoader;

public class CustomLauncherInterceptor implements LauncherInterceptor {

    private static int count = 0;
    private static int constructCount = 0;
    FacadeClassLoader facadeLoader = null;

    public CustomLauncherInterceptor() throws Exception {
        System.out.println(constructCount++ + "HOLLY interceipt constructor" + getClass().getClassLoader());
        ClassLoader parent = Thread.currentThread()
                .getContextClassLoader();
        System.out.println("HOLLY at interceptor construction, CCL is " + parent);

    }

    @Override
    public <T> T intercept(Invocation<T> invocation) {
        System.out.println("HOLLY intercept top level" + invocation);
        if (System.getProperty("prod.mode.tests") != null) {
            return invocation.proceed();

        } else {
            try {
                return nintercept(invocation);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    private <T> T nintercept(Invocation<T> invocation) {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        // Don't make a facade loader if the JUnitRunner got there ahead of us
        // they set a runtime classloader so handle that too
        // TODO actually, since it's a singleton, we could skip the check
        // Be aware, this method might be called more than once, for different kinds of invocations; especially for Gradle executions, the executions could happen before the TCCL gets constructed and set
        if (!(old instanceof FacadeClassLoader)) {
            System.out.println(
                    "HOLLY intercept constructing a classloader ------------------------------" + Thread.currentThread());
            try {
                // TODO we should be able to do better than this here
                //TODO  We want to tidy up classloaders we created, but not ones created upstream
                facadeLoader = null; // TODO diagnostics
                // Although in principle we only go through a few times
                // TODO we should probably drop the parameter
                if (facadeLoader == null) {
                    facadeLoader = FacadeClassLoader.instance(old); // TODO want to do it reflectively CollaboratingClassLoader.construct(old);
                }
                Thread.currentThread()
                        .setContextClassLoader(facadeLoader);
                System.out.println("HOLLY did set TCCL " + Thread.currentThread());
                return invocation.proceed();
            } finally {
                System.out.println("HOLLY doing finally, setting back to  " + old);

                // TODO It would clearly be nice to tidy up, but I think the gradle
                // devtools tests may be asynchronous, because if we reset the TCCL
                // at this point, the test loads with the wrong classloader
                //                Thread.currentThread()
                //                        .setContextClassLoader(old);
            }
        } else {
            return invocation.proceed();
        }
    }

    @Override
    public void close() {

        try {
            // Tidy up classloaders we created, but not ones created upstream
            if (facadeLoader != null) {
                facadeLoader.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to close custom classloader", e);
        }
    }
}
