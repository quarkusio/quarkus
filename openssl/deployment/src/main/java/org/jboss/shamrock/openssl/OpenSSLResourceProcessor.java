package org.jboss.shamrock.openssl;

import org.jboss.shamrock.deployment.ArchiveContext;
import org.jboss.shamrock.deployment.ProcessorContext;
import org.jboss.shamrock.deployment.ResourceProcessor;

public class OpenSSLResourceProcessor implements ResourceProcessor {


    @Override
    public void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception {
        processorContext.addRuntimeInitializedClasses("org.wildfly.openssl.OpenSSLEngine");
    }

    @Override
    public int getPriority() {
        return 100;
    }

}
