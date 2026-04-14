package org.acme.app;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/tree-shake/jni")
public class JniResource {
    @GET
    public String check() throws Exception {
        Class<?> clazz = Class.forName("org.acme.libjni.JniTarget");
        Object instance = clazz.getDeclaredConstructor().newInstance();
        return clazz.getMethod("getName").invoke(instance).toString();
    }
}
