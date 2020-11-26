package org.jboss.resteasy.reactive.common.model;

import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;
import org.jboss.resteasy.reactive.SseElementType;
import org.jboss.resteasy.reactive.spi.EndpointInvoker;

/**
 * A representation of a REST endpoint. This is passed directly to recorders so must be bytecode serializable.
 */
public class ResourceMethod {

    /**
     * The HTTP Method. Will be null if this is a resource locator method. Note that only one method is allowed in this class,
     * if a method has multiple method annotations then it should be added multiple times.
     */
    private String httpMethod;

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
     * The value of the {@link SseElementType} annotation, if none is specified on the method
     * then this represents the value inherited from the class level, or null if not specified.
     */
    private String sseElementType;

    /**
     * The value of the {@link javax.ws.rs.Consumes} annotation, if none is specified on the method
     * then this represents the value inherited from the class level, or null if not specified.
     */
    private String[] consumes;

    /**
     * The class names of the {@code @NameBinding} annotations that the method is annotated with.
     * If none is specified on the method then this represents the value inherited from the class level.
     */
    private Set<String> nameBindingNames = Collections.emptySet();

    private Supplier<EndpointInvoker> invoker;

    private String name;

    private String returnType;
    private String simpleReturnType;

    private MethodParameter[] parameters;

    private boolean blocking;

    private boolean suspended;

    private boolean isSse;

    private boolean isFormParamRequired;

    public boolean isResourceLocator() {
        return httpMethod == null;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public ResourceMethod setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
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

    public Set<String> getNameBindingNames() {
        return nameBindingNames;
    }

    public ResourceMethod setNameBindingNames(Set<String> nameBindingNames) {
        this.nameBindingNames = nameBindingNames;
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

    public String getSimpleReturnType() {
        return simpleReturnType;
    }

    public ResourceMethod setSimpleReturnType(String simpleReturnType) {
        this.simpleReturnType = simpleReturnType;
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

    public boolean isSuspended() {
        return suspended;
    }

    public ResourceMethod setSuspended(boolean suspended) {
        this.suspended = suspended;
        return this;
    }

    public boolean isSse() {
        return isSse;
    }

    public ResourceMethod setSse(boolean isSse) {
        this.isSse = isSse;
        return this;
    }

    public boolean isFormParamRequired() {
        return isFormParamRequired;
    }

    public ResourceMethod setFormParamRequired(boolean isFormParamRequired) {
        this.isFormParamRequired = isFormParamRequired;
        return this;
    }

    public ResourceMethod setSseElementType(String sseElementType) {
        this.sseElementType = sseElementType;
        return this;
    }

    public String getSseElementType() {
        return sseElementType;
    }

    @Override
    public String toString() {
        if (httpMethod != null)
            return httpMethod + " " + path;
        return "Sub-resource locator for " + path;
    }
}
