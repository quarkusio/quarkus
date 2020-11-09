package org.jboss.resteasy.reactive.common.processor;

import java.util.Map;
import java.util.Set;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.common.model.ParameterType;

public class IndexedParameter<T extends IndexedParameter<T>> {
    protected ClassInfo currentClassInfo;
    protected ClassInfo actualEndpointInfo;
    protected Map<String, String> existingConverters;
    protected AdditionalReaders additionalReaders;
    protected Map<DotName, AnnotationInstance> anns;
    protected Type paramType;
    protected String errorLocation;
    protected boolean hasRuntimeConverters;
    protected Set<String> pathParameters;
    protected String sourceName;
    protected boolean field;
    protected boolean suspended;
    protected boolean sse;
    protected String name;
    protected String defaultValue;
    protected ParameterType type;
    protected String elementType;
    protected boolean single;

    public boolean isObtainedAsCollection() {
        return !single
                && (type == ParameterType.HEADER
                        || type == ParameterType.MATRIX
                        || type == ParameterType.FORM
                        || type == ParameterType.QUERY);
    }

    public ClassInfo getCurrentClassInfo() {
        return currentClassInfo;
    }

    public T setCurrentClassInfo(ClassInfo currentClassInfo) {
        this.currentClassInfo = currentClassInfo;
        return (T) this;
    }

    public ClassInfo getActualEndpointInfo() {
        return actualEndpointInfo;
    }

    public T setActualEndpointInfo(ClassInfo actualEndpointInfo) {
        this.actualEndpointInfo = actualEndpointInfo;
        return (T) this;
    }

    public Map<String, String> getExistingConverters() {
        return existingConverters;
    }

    public T setExistingConverters(Map<String, String> existingConverters) {
        this.existingConverters = existingConverters;
        return (T) this;
    }

    public AdditionalReaders getAdditionalReaders() {
        return additionalReaders;
    }

    public T setAdditionalReaders(AdditionalReaders additionalReaders) {
        this.additionalReaders = additionalReaders;
        return (T) this;
    }

    public Map<DotName, AnnotationInstance> getAnns() {
        return anns;
    }

    public T setAnns(Map<DotName, AnnotationInstance> anns) {
        this.anns = anns;
        return (T) this;
    }

    public Type getParamType() {
        return paramType;
    }

    public T setParamType(Type paramType) {
        this.paramType = paramType;
        return (T) this;
    }

    public String getErrorLocation() {
        return errorLocation;
    }

    public T setErrorLocation(String errorLocation) {
        this.errorLocation = errorLocation;
        return (T) this;
    }

    public boolean isHasRuntimeConverters() {
        return hasRuntimeConverters;
    }

    public T setHasRuntimeConverters(boolean hasRuntimeConverters) {
        this.hasRuntimeConverters = hasRuntimeConverters;
        return (T) this;
    }

    public Set<String> getPathParameters() {
        return pathParameters;
    }

    public T setPathParameters(Set<String> pathParameters) {
        this.pathParameters = pathParameters;
        return (T) this;
    }

    public String getSourceName() {
        return sourceName;
    }

    public T setSourceName(String sourceName) {
        this.sourceName = sourceName;
        return (T) this;
    }

    public boolean isField() {
        return field;
    }

    public T setField(boolean field) {
        this.field = field;
        return (T) this;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public T setSuspended(boolean suspended) {
        this.suspended = suspended;
        return (T) this;
    }

    public boolean isSse() {
        return sse;
    }

    public T setSse(boolean sse) {
        this.sse = sse;
        return (T) this;
    }

    public String getName() {
        return name;
    }

    public T setName(String name) {
        this.name = name;
        return (T) this;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public T setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return (T) this;
    }

    public ParameterType getType() {
        return type;
    }

    public T setType(ParameterType type) {
        this.type = type;
        return (T) this;
    }

    public String getElementType() {
        return elementType;
    }

    public T setElementType(String elementType) {
        this.elementType = elementType;
        return (T) this;
    }

    public boolean isSingle() {
        return single;
    }

    public T setSingle(boolean single) {
        this.single = single;
        return (T) this;
    }

}
