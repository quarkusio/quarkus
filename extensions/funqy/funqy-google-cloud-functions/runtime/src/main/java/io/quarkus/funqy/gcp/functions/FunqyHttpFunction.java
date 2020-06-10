package io.quarkus.funqy.gcp.functions;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;

import io.quarkus.runtime.Application;

public class FunqyHttpFunction implements HttpFunction {
    protected static final String deploymentStatus;
    protected static boolean started = false;

    static {
        StringWriter error = new StringWriter();
        PrintWriter errorWriter = new PrintWriter(error, true);
        if (Application.currentApplication() == null) { // were we already bootstrapped?  Needed for mock unit testing.
            // For GCP functions, we need to set the TCCL to the QuarkusHttpFunction classloader then restore it.
            // Without this, we have a lot of classloading issues (ClassNotFoundException on existing classes)
            // during static init.
            ClassLoader currentCl = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(FunqyHttpFunction.class.getClassLoader());
                Class<?> appClass = Class.forName("io.quarkus.runner.ApplicationImpl");
                String[] args = {};
                Application app = (Application) appClass.newInstance();
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    @Override
                    public void run() {
                        app.stop();
                    }
                });
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

    @Override
    public void service(HttpRequest request, HttpResponse response) throws IOException {
        if (!started) {
            throw new IOException(deploymentStatus);
        }
        FunqyCloudFunctionsBindingRecorder.handle(request, response);
    }
}
