package io.quarkus.it.arc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.arc.ClientProxy;

@Path("/reflective-bean")
public class ReflectiveBeanEndpoint {

    @Inject
    IntercepredNormalScopedFoo foo;

    @Path("proxy")
    @GET
    public int proxyPing() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
        return callPingViaReflection(foo);
    }

    @Path("subclass")
    @GET
    public int subclassPing() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
        IntercepredNormalScopedFoo subclass = (IntercepredNormalScopedFoo) ((ClientProxy) foo).arc_contextualInstance();
        return callPingViaReflection(subclass);
    }

    private int callPingViaReflection(IntercepredNormalScopedFoo foo) throws NoSuchMethodException, SecurityException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method pingMethod = foo.getClass().getDeclaredMethod("ping");
        return (int) pingMethod.invoke(foo);
    }
}
