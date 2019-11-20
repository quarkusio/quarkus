package io.quarkus.it.nativeannotations;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.quarkus.runtime.annotations.RegisterProxy;

@Path("/native-proxy")
@RegisterProxy(ProxyInterfaceOne.class)
@RegisterProxy(ProxyInterfaceTwo.class)
public class ProxyResource {

    @GET
    public String result() {
        ProxyInterfaceTwo proxy = (ProxyInterfaceTwo) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[] { ProxyInterfaceTwo.class },
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        return "passed";
                    }
                });
        return proxy.message();
    }

}
