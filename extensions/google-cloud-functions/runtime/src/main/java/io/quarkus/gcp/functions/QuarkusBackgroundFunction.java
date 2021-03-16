package io.quarkus.gcp.functions;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;

import com.fasterxml.jackson.core.JsonParseException;
import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.google.cloud.functions.RawBackgroundFunction;
import com.google.gson.Gson;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.Application;

public final class QuarkusBackgroundFunction implements RawBackgroundFunction {

    protected static final String deploymentStatus;
    protected static boolean started = false;

    private static volatile BackgroundFunction delegate;
    private static volatile Class<?> parameterType;
    private static volatile RawBackgroundFunction rawDelegate;

    static {
        StringWriter error = new StringWriter();
        PrintWriter errorWriter = new PrintWriter(error, true);
        if (Application.currentApplication() == null) { // were we already bootstrapped?  Needed for mock unit testing.
            ClassLoader currentCl = Thread.currentThread().getContextClassLoader();
            try {
                // For GCP functions, we need to set the TCCL to the QuarkusHttpFunction classloader then restore it.
                // Without this, we have a lot of classloading issues (ClassNotFoundException on existing classes)
                // during static init.
                Thread.currentThread().setContextClassLoader(QuarkusBackgroundFunction.class.getClassLoader());
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

    static void setDelegates(String selectedDelegate, String selectedRawDelegate) {
        if (selectedDelegate != null) {
            try {
                Class<?> clazz = Class.forName(selectedDelegate, false, Thread.currentThread().getContextClassLoader());
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.getName().equals("accept")) {
                        // the first parameter of the accept method is the event, we need to register it's type to
                        // be able to deserialize to it to mimic what a BackgroundFunction does
                        if (method.getParameterTypes()[0] != Object.class) {// FIXME we have two accept methods !!!
                            parameterType = method.getParameterTypes()[0];
                        }
                    }
                }
                delegate = (BackgroundFunction) Arc.container().instance(clazz).get();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        if (selectedRawDelegate != null) {
            try {
                Class<?> clazz = Class.forName(selectedRawDelegate, false, Thread.currentThread().getContextClassLoader());
                rawDelegate = (RawBackgroundFunction) Arc.container().instance(clazz).get();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void accept(String event, Context context) throws Exception {
        if (!started) {
            throw new IOException(deploymentStatus);
        }

        // TODO maybe we can check this at static init
        if ((delegate == null && rawDelegate == null) || (delegate != null && rawDelegate != null)) {
            throw new IOException("We didn't found any BackgroundFunction or RawBackgroundFunction to run " +
                    "(or there is multiple one and none selected inside your application.properties)");
        }

        if (rawDelegate != null) {
            rawDelegate.accept(event, context);
        } else {
            // here we emulate a BackgroundFunction from a RawBackgroundFunction
            // so we need to un-marshall the JSON into the delegate parameter type
            // this is done via Gson as it's the library that GCF uses internally for the same purpose
            // see NewBackgroundFunctionExecutor.TypedFunctionExecutor<T>
            // from the functions-framework-java https://github.com/GoogleCloudPlatform/functions-framework-java
            Gson gson = new Gson();
            try {
                Object eventObj = gson.fromJson(event, parameterType);
                delegate.accept(eventObj, context);
            } catch (JsonParseException e) {
                throw new RuntimeException("Could not parse received event payload into type "
                        + parameterType.getCanonicalName(), e);
            }
        }
    }
}
