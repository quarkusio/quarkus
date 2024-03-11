package io.quarkus.resteasy.reactive.server.deployment;

import java.util.List;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.resteasy.reactive.common.model.ResourceMethod;
import org.jboss.resteasy.reactive.common.processor.EndpointIndexer;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Provides a list of entries for each JAX-RS Resource Methods created during the indexing process.
 * Each entry also contains the information about the Java class and method that correspond
 * to the JAX-RS Resource Method, giving extensions access to the entire set of metadata
 * thus allowing them to build additionally build-time functionality.
 */
public final class ResteasyReactiveResourceMethodEntriesBuildItem extends SimpleBuildItem {

    private final List<Entry> entries;

    public ResteasyReactiveResourceMethodEntriesBuildItem(List<Entry> entries) {
        this.entries = entries;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public static class Entry {
        private final EndpointIndexer.BasicResourceClassInfo basicResourceClassInfo;
        private final MethodInfo methodInfo;
        private final ClassInfo actualClassInfo;
        private final ResourceMethod resourceMethod;

        public Entry(EndpointIndexer.BasicResourceClassInfo basicResourceClassInfo, MethodInfo methodInfo,
                ClassInfo actualClassInfo, ResourceMethod resourceMethod) {
            this.basicResourceClassInfo = basicResourceClassInfo;
            this.methodInfo = methodInfo;
            this.actualClassInfo = actualClassInfo;
            this.resourceMethod = resourceMethod;
        }

        public EndpointIndexer.BasicResourceClassInfo getBasicResourceClassInfo() {
            return basicResourceClassInfo;
        }

        public MethodInfo getMethodInfo() {
            return methodInfo;
        }

        public ClassInfo getActualClassInfo() {
            return actualClassInfo;
        }

        public ResourceMethod getResourceMethod() {
            return resourceMethod;
        }
    }
}
