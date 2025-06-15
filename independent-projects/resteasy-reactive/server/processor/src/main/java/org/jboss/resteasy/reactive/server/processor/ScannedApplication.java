package org.jboss.resteasy.reactive.server.processor;

import java.util.List;

import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.common.model.ResourceInterceptors;
import org.jboss.resteasy.reactive.common.processor.AdditionalReaders;
import org.jboss.resteasy.reactive.common.processor.AdditionalWriters;
import org.jboss.resteasy.reactive.common.processor.scanning.ApplicationScanningResult;
import org.jboss.resteasy.reactive.common.processor.scanning.ResourceScanningResult;
import org.jboss.resteasy.reactive.common.processor.scanning.SerializerScanningResult;
import org.jboss.resteasy.reactive.server.core.ExceptionMapping;
import org.jboss.resteasy.reactive.server.model.ContextResolvers;
import org.jboss.resteasy.reactive.server.model.DynamicFeatures;
import org.jboss.resteasy.reactive.server.model.Features;
import org.jboss.resteasy.reactive.server.model.ParamConverterProviders;

public class ScannedApplication {

    final ResourceScanningResult resourceScanningResult;
    final AdditionalReaders readers;
    final AdditionalWriters writers;
    final SerializerScanningResult serializerScanningResult;
    final ApplicationScanningResult applicationScanningResult;
    final List<ResourceClass> resourceClasses;
    final List<ResourceClass> subResourceClasses;
    final Features scannedFeatures;
    final ResourceInterceptors resourceInterceptors;
    final DynamicFeatures dynamicFeatures;
    final ParamConverterProviders paramConverters;
    final ExceptionMapping exceptionMappers;
    final ContextResolvers contextResolvers;

    public ScannedApplication(ResourceScanningResult resourceScanningResult, AdditionalReaders readers,
            AdditionalWriters writers, SerializerScanningResult serializerScanningResult,
            ApplicationScanningResult applicationScanningResult, List<ResourceClass> resourceClasses,
            List<ResourceClass> subResourceClasses, Features scannedFeatures, ResourceInterceptors resourceInterceptors,
            DynamicFeatures dynamicFeatures, ParamConverterProviders paramConverters, ExceptionMapping exceptionMappers,
            ContextResolvers contextResolvers) {
        this.resourceScanningResult = resourceScanningResult;
        this.readers = readers;
        this.writers = writers;
        this.serializerScanningResult = serializerScanningResult;
        this.applicationScanningResult = applicationScanningResult;
        this.resourceClasses = resourceClasses;
        this.subResourceClasses = subResourceClasses;
        this.scannedFeatures = scannedFeatures;
        this.resourceInterceptors = resourceInterceptors;
        this.dynamicFeatures = dynamicFeatures;
        this.paramConverters = paramConverters;
        this.exceptionMappers = exceptionMappers;
        this.contextResolvers = contextResolvers;
    }

    public ResourceScanningResult getResourceScanningResult() {
        return resourceScanningResult;
    }

    public AdditionalReaders getReaders() {
        return readers;
    }

    public AdditionalWriters getWriters() {
        return writers;
    }

    public SerializerScanningResult getSerializerScanningResult() {
        return serializerScanningResult;
    }

    public ApplicationScanningResult getApplicationScanningResult() {
        return applicationScanningResult;
    }

    public List<ResourceClass> getResourceClasses() {
        return resourceClasses;
    }

    public List<ResourceClass> getSubResourceClasses() {
        return subResourceClasses;
    }

    public Features getScannedFeatures() {
        return scannedFeatures;
    }

    public ResourceInterceptors getResourceInterceptors() {
        return resourceInterceptors;
    }

    public DynamicFeatures getDynamicFeatures() {
        return dynamicFeatures;
    }

    public ParamConverterProviders getParamConverters() {
        return paramConverters;
    }

    public ExceptionMapping getExceptionMappers() {
        return exceptionMappers;
    }

    public ContextResolvers getContextResolvers() {
        return contextResolvers;
    }
}
