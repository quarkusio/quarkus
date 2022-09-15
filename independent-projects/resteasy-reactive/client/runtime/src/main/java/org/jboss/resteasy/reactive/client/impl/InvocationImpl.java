package org.jboss.resteasy.reactive.client.impl;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.InvocationCallback;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class InvocationImpl implements Invocation {

    final AsyncInvokerImpl invoker;
    final String method;
    final Entity<?> entity;

    public InvocationImpl(String method, AsyncInvokerImpl invoker, Entity<?> entity) {
        this.invoker = invoker;
        this.method = method;
        this.entity = entity;
    }

    @Override
    public Invocation property(String name, Object value) {
        invoker.properties.put(name, value);
        return this;
    }

    @Override
    public Response invoke() {
        try {
            return invoker.method(method, entity).get();
        } catch (InterruptedException e) {
            throw new ProcessingException(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ProcessingException) {
                throw (ProcessingException) e.getCause();
            }
            throw new RuntimeException(e.getCause());
        }
    }

    @Override
    public <T> T invoke(Class<T> responseType) {
        try {
            return invoker.method(method, entity, responseType).get();
        } catch (InterruptedException e) {
            throw new ProcessingException(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ProcessingException) {
                throw (ProcessingException) e.getCause();
            }
            if (e.getCause() instanceof WebApplicationException) {
                throw (WebApplicationException) e.getCause();
            }
            throw new RuntimeException(e.getCause());
        }
    }

    @Override
    public <T> T invoke(GenericType<T> responseType) {
        try {
            return invoker.method(method, entity, responseType).get();
        } catch (InterruptedException e) {
            throw new ProcessingException(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ProcessingException) {
                throw (ProcessingException) e.getCause();
            }
            if (e.getCause() instanceof WebApplicationException) {
                throw (WebApplicationException) e.getCause();
            }
            throw new RuntimeException(e.getCause());
        }
    }

    @Override
    public Future<Response> submit() {
        return invoker.method(method, entity);
    }

    @Override
    public <T> Future<T> submit(Class<T> responseType) {
        return invoker.method(method, entity, responseType);
    }

    @Override
    public <T> Future<T> submit(GenericType<T> responseType) {
        return invoker.method(method, entity, responseType);
    }

    @Override
    public <T> Future<T> submit(InvocationCallback<T> callback) {
        return invoker.method(method, entity, callback);
    }
}
