package io.quarkus.gcp.functions;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.google.cloud.functions.CloudEventsFunction;

import io.cloudevents.CloudEvent;
import io.quarkus.arc.Arc;
import io.quarkus.runtime.Application;

public final class QuarkusCloudEventsFunction implements CloudEventsFunction {

    protected static final String deploymentStatus;
    protected static boolean started = false;

    private static volatile CloudEventsFunction delegate;

    static {
        StringWriter error = new StringWriter();
        PrintWriter errorWriter = new PrintWriter(error, true);
        if (Application.currentApplication() == null) { // were we already bootstrapped? Needed for mock unit testing.
            ClassLoader currentCl = Thread.currentThread().getContextClassLoader();
            try {
                // For GCP functions, we need to set the TCCL to the QuarkusHttpFunction classloader then restore it.
                // Without this, we have a lot of classloading issues (ClassNotFoundException on existing classes)
                // during static init.
                Thread.currentThread().setContextClassLoader(QuarkusCloudEventsFunction.class.getClassLoader());
                Class<?> appClass = Class.forName("io.quarkus.runner.ApplicationImpl");
                String[] args = {};
                Application app = (Application) appClass.getConstructor().newInstance();
                app.start(args);
                errorWriter.println("Quarkus bootstrapped successfully.");
                started = true;
            } catch (Exception ex) {
                errorWriter.println("Quarkus bootstrap failed.");
                ex.printStackTrace(errorWriter);
            } finally {
                Thread.currentThread().setContextClassLoader(currentCl);
            }
        } else {
            errorWriter.println("Quarkus bootstrapped successfully.");
            started = true;
        }
        deploymentStatus = error.toString();
    }

    static void setDelegate(String selectedDelegate) {
        if (selectedDelegate != null) {
            try {
                Class<?> clazz = Class.forName(selectedDelegate, false, Thread.currentThread().getContextClassLoader());
                delegate = (CloudEventsFunction) Arc.container().instance(clazz).get();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void accept(CloudEvent cloudEvent) throws Exception {
        if (!started) {
            throw new IOException(deploymentStatus);
        }

        // TODO maybe we can check this at static init
        if (delegate == null) {
            throw new IOException("We didn't found any CloudEventsFunction to run "
                    + "(or there is multiple one and none selected inside your application.properties)");
        }

        delegate.accept(cloudEvent);
    }
}
