package org.jboss.shamrock.camel.deployment;

import org.jboss.shamrock.deployment.ArchiveContext;
import org.jboss.shamrock.deployment.ProcessorContext;
import org.jboss.shamrock.deployment.ResourceProcessor;

public class CamelProcessor implements ResourceProcessor {

    @Override
    public void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception {

    }

    @Override
    public int getPriority() {
        return 0;
    }
}
