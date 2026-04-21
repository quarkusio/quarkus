package org.acme.app;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.runtime.annotations.RegisterForReflection;

@Path("/tree-shake/reflection")
@RegisterForReflection(targets = {org.acme.libreflection.ReflectionTarget.class})
public class ReflectionResource {
    @GET
    public String check() throws Exception {
        Class<?> clazz = Class.forName("org.acme.libreflection.ReflectionTarget");
        Object instance = clazz.getDeclaredConstructor().newInstance();
        return clazz.getMethod("getName").invoke(instance).toString();
    }
}
