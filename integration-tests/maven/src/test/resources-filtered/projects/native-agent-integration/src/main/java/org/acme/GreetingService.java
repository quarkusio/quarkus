package org.acme;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;

import java.lang.reflect.Method;

@ApplicationScoped
public class GreetingService
{
    public String greeting(String name)
    {
        try
        {
            final Class<?> clazz = Class.forName("org.acme." + name);
            final Method method = clazz.getMethod("sayMyName");
            final Object obj = clazz.getDeclaredConstructor().newInstance();
            final Object result = method.invoke(obj);
            return "Hello " + result;
        }
        catch (Exception e)
        {
            Log.debugf(e, "Unable to create a greeting");
            throw new NotFoundException("Unknown name: " + name);
        }
    }
}
