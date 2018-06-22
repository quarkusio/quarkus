package org.jboss.shamrock.core;

import java.util.Collections;
import java.util.Set;

import org.jboss.jandex.DotName;

public interface ResourceProcessor {

    void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception;

    default Set<DotName> getProcessedAnnotations() {
        return Collections.emptySet();
    }

    default Set<String> getProcessedResources() {
        return Collections.emptySet();
    }

    default boolean shouldRunIfNoResources() {
        return false;
    }

    //TODO: remove this
    int getPriority();
}
