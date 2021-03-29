package org.jboss.resteasy.reactive.common.processor.scanning;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

public final class ResourceScanningResult {

    private final IndexView index;
    final Map<DotName, ClassInfo> scannedResources;
    final Map<DotName, String> scannedResourcePaths;
    final Map<DotName, ClassInfo> possibleSubResources;
    final Map<DotName, String> pathInterfaces;
    final Map<DotName, String> clientInterfaces;
    final Map<DotName, MethodInfo> resourcesThatNeedCustomProducer;
    final Set<String> beanParams;
    final Map<DotName, String> httpAnnotationToMethod;
    final List<MethodInfo> classLevelExceptionMappers;

    public ResourceScanningResult(IndexView index, Map<DotName, ClassInfo> scannedResources,
            Map<DotName, String> scannedResourcePaths,
            Map<DotName, ClassInfo> possibleSubResources, Map<DotName, String> pathInterfaces,
            Map<DotName, String> clientInterfaces,
            Map<DotName, MethodInfo> resourcesThatNeedCustomProducer,
            Set<String> beanParams, Map<DotName, String> httpAnnotationToMethod, List<MethodInfo> classLevelExceptionMappers) {
        this.index = index;
        this.scannedResources = scannedResources;
        this.scannedResourcePaths = scannedResourcePaths;
        this.possibleSubResources = possibleSubResources;
        this.pathInterfaces = pathInterfaces;
        this.clientInterfaces = clientInterfaces;
        this.resourcesThatNeedCustomProducer = resourcesThatNeedCustomProducer;
        this.beanParams = beanParams;
        this.httpAnnotationToMethod = httpAnnotationToMethod;
        this.classLevelExceptionMappers = classLevelExceptionMappers;
    }

    public IndexView getIndex() {
        return index;
    }

    public Map<DotName, ClassInfo> getScannedResources() {
        return scannedResources;
    }

    public Map<DotName, String> getScannedResourcePaths() {
        return scannedResourcePaths;
    }

    public Map<DotName, ClassInfo> getPossibleSubResources() {
        return possibleSubResources;
    }

    public Map<DotName, String> getPathInterfaces() {
        return pathInterfaces;
    }

    public Map<DotName, String> getClientInterfaces() {
        return clientInterfaces;
    }

    public Map<DotName, MethodInfo> getResourcesThatNeedCustomProducer() {
        return resourcesThatNeedCustomProducer;
    }

    public Set<String> getBeanParams() {
        return beanParams;
    }

    public Map<DotName, String> getHttpAnnotationToMethod() {
        return httpAnnotationToMethod;
    }

    public List<MethodInfo> getClassLevelExceptionMappers() {
        return classLevelExceptionMappers;
    }
}
