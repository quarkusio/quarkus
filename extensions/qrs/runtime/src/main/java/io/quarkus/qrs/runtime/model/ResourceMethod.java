package io.quarkus.qrs.runtime.model;

import java.util.function.Supplier;

import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import io.quarkus.qrs.runtime.spi.EndpointInvoker;

/**
 * A representation of a REST endpoint. This is passed directly to recorders so must be bytecode serializable.
 */
public class ResourceMethod {

    /**
     * The HTTP Method. Will be null if this is a resource locator method. Note that only one method is allowed in this class,
     * if a method has multiple method annotations then it should be added multiple times.
     */
    private String method;

    /**
     * The value of the path annotation. May be null if this serves from the path of the resource class.
     */
    private String path;

    /**
     * The value of the {@link javax.ws.rs.Produces} annotation, if none is specified on the method
     * then this represents the value inherited from the class level, or null if not specified.
     */
    private String[] produces;

    /**
     * The value of the {@link javax.ws.rs.Consumes} annotation, if none is specified on the method
     * then this represents the value inherited from the class level, or null if not specified.
     */
    private String[] consumes;

    private Supplier<EndpointInvoker> invoker;

    private String name;

    private String returnType;

    private MethodParameter[] parameters;

    private boolean blocking;

    //TODO: fix this
    private MessageBodyWriter<?> writer;
    private MessageBodyReader<?> reader;

    public boolean isResourceLocator() {
        return method == null;
    }

    public String getMethod() {
        return method;
    }

    public ResourceMethod setMethod(String method) {
        this.method = method;
        return this;
    }

    public String getPath() {
        return path;
    }

    public ResourceMethod setPath(String path) {
        this.path = path;
        return this;
    }

    public String[] getProduces() {
        return produces;
    }

    public ResourceMethod setProduces(String[] produces) {
        this.produces = produces;
        return this;
    }

    public String[] getConsumes() {
        return consumes;
    }

    public ResourceMethod setConsumes(String[] consumes) {
        this.consumes = consumes;
        return this;
    }

    public String getName() {
        return name;
    }

    public ResourceMethod setName(String name) {
        this.name = name;
        return this;
    }

    public String getReturnType() {
        return returnType;
    }

    public ResourceMethod setReturnType(String returnType) {
        this.returnType = returnType;
        return this;
    }

    public MethodParameter[] getParameters() {
        return parameters;
    }

    public ResourceMethod setParameters(MethodParameter[] parameters) {
        this.parameters = parameters;
        return this;
    }

    public Supplier<EndpointInvoker> getInvoker() {
        return invoker;
    }

    public ResourceMethod setInvoker(Supplier<EndpointInvoker> invoker) {
        this.invoker = invoker;
        return this;
    }

    public boolean isBlocking() {
        return blocking;
    }

    public ResourceMethod setBlocking(boolean blocking) {
        this.blocking = blocking;
        return this;
    }
}
