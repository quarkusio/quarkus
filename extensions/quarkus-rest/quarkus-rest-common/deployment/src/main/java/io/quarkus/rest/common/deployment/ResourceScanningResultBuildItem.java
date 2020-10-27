package io.quarkus.rest.common.deployment;

import java.util.Map;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

import io.quarkus.builder.item.SimpleBuildItem;

public final class ResourceScanningResultBuildItem extends SimpleBuildItem {

    final Map<DotName, ClassInfo> scannedResources;
    final Map<DotName, String> scannedResourcePaths;
    final Map<DotName, ClassInfo> possibleSubResources;
    final Map<DotName, String> pathInterfaces;
    final Map<DotName, MethodInfo> resourcesThatNeedCustomProducer;
    final Set<String> beanParams;

    public ResourceScanningResultBuildItem(Map<DotName, ClassInfo> scannedResources, Map<DotName, String> scannedResourcePaths,
            Map<DotName, ClassInfo> possibleSubResources, Map<DotName, String> pathInterfaces,
            Map<DotName, MethodInfo> resourcesThatNeedCustomProducer,
            Set<String> beanParams) {
        this.scannedResources = scannedResources;
        this.scannedResourcePaths = scannedResourcePaths;
        this.possibleSubResources = possibleSubResources;
        this.pathInterfaces = pathInterfaces;
        this.resourcesThatNeedCustomProducer = resourcesThatNeedCustomProducer;
        this.beanParams = beanParams;
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

    public Map<DotName, MethodInfo> getResourcesThatNeedCustomProducer() {
        return resourcesThatNeedCustomProducer;
    }

    public Set<String> getBeanParams() {
        return beanParams;
    }
}
